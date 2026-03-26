package com.triptally.service;

import com.triptally.domain.entity.Expense;
import com.triptally.domain.entity.ReceiptAttachment;
import com.triptally.domain.entity.User;
import com.triptally.exception.ApiException;
import com.triptally.repository.ExpenseRepository;
import com.triptally.repository.ReceiptAttachmentRepository;
import com.triptally.service.storage.LocalReceiptStorageService;
import com.triptally.service.storage.StoredReceipt;
import java.io.IOException;
import java.nio.file.Files;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReceiptService {

	private final TripAccessService tripAccessService;
	private final ExpenseRepository expenseRepository;
	private final ReceiptAttachmentRepository receiptAttachmentRepository;
	private final LocalReceiptStorageService storageService;

	public ReceiptService(
			TripAccessService tripAccessService,
			ExpenseRepository expenseRepository,
			ReceiptAttachmentRepository receiptAttachmentRepository,
			LocalReceiptStorageService storageService) {
		this.tripAccessService = tripAccessService;
		this.expenseRepository = expenseRepository;
		this.receiptAttachmentRepository = receiptAttachmentRepository;
		this.storageService = storageService;
	}

	@Transactional
	public void upload(Long expenseId, User user, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a receipt image to upload");
		}
		Expense expense = expenseRepository.findById(expenseId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expense not found"));
		tripAccessService.requireTripMember(expense.getTrip().getId(), user);
		receiptAttachmentRepository.findByExpense(expense).ifPresent(existing -> {
			try {
				Files.deleteIfExists(storageService.resolvePath(existing.getFilePath()));
			}
			catch (IOException ignored) {
			}
			receiptAttachmentRepository.delete(existing);
		});
		try {
			StoredReceipt stored = storageService.store(file, expense.getTrip().getId(), expenseId);
			ReceiptAttachment attachment = ReceiptAttachment.builder()
					.expense(expense)
					.filePath(stored.getRelativePath())
					.contentType(stored.getContentType())
					.originalFilename(stored.getOriginalFilename())
					.build();
			receiptAttachmentRepository.save(attachment);
		}
		catch (IOException e) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store receipt");
		}
	}

	@Transactional(readOnly = true)
	public ResourceWithType load(Long expenseId, User user) {
		Expense expense = expenseRepository.findById(expenseId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expense not found"));
		tripAccessService.requireTripMember(expense.getTrip().getId(), user);
		ReceiptAttachment attachment = receiptAttachmentRepository.findByExpense(expense)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No receipt for this expense"));
		try {
			var path = storageService.resolvePath(attachment.getFilePath());
			Resource resource = new UrlResource(path.toUri());
			if (!resource.exists() || !resource.isReadable()) {
				throw new ApiException(HttpStatus.NOT_FOUND, "Receipt file is missing on disk");
			}
			return new ResourceWithType(resource, attachment.getContentType(), attachment.getOriginalFilename());
		}
		catch (IOException e) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Receipt file is missing on disk");
		}
	}

	public record ResourceWithType(Resource resource, String contentType, String filename) {
	}
}
