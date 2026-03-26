package com.triptally.controller;

import com.triptally.dto.expense.ExpenseResponse;
import com.triptally.dto.expense.ExpenseUpdateRequest;
import com.triptally.service.CurrentUserService;
import com.triptally.service.ExpenseService;
import com.triptally.service.ReceiptService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

	private final ExpenseService expenseService;
	private final ReceiptService receiptService;
	private final CurrentUserService currentUserService;

	public ExpenseController(
			ExpenseService expenseService,
			ReceiptService receiptService,
			CurrentUserService currentUserService) {
		this.expenseService = expenseService;
		this.receiptService = receiptService;
		this.currentUserService = currentUserService;
	}

	@GetMapping("/{expenseId}")
	public ExpenseResponse get(@PathVariable Long expenseId) {
		return expenseService.get(expenseId, currentUserService.requireUser());
	}

	@PutMapping("/{expenseId}")
	public ExpenseResponse update(@PathVariable Long expenseId, @Valid @RequestBody ExpenseUpdateRequest request) {
		return expenseService.update(expenseId, currentUserService.requireUser(), request);
	}

	@DeleteMapping("/{expenseId}")
	public ResponseEntity<Void> delete(@PathVariable Long expenseId) {
		expenseService.delete(expenseId, currentUserService.requireUser());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{expenseId}/receipt")
	public ResponseEntity<Void> uploadReceipt(@PathVariable Long expenseId, @RequestParam("file") MultipartFile file) {
		receiptService.upload(expenseId, currentUserService.requireUser(), file);
		return ResponseEntity.accepted().build();
	}

	@GetMapping("/{expenseId}/receipt")
	public ResponseEntity<Resource> downloadReceipt(@PathVariable Long expenseId) {
		ReceiptService.ResourceWithType r = receiptService.load(expenseId, currentUserService.requireUser());
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(r.contentType()))
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + r.filename() + "\"")
				.body(r.resource());
	}
}
