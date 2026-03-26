package com.tripTally.service.storage;

import com.tripTally.config.TriptallyProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalReceiptStorageService {

	private final TriptallyProperties properties;

	public LocalReceiptStorageService(TriptallyProperties properties) {
		this.properties = properties;
	}

	public StoredReceipt store(MultipartFile file, long tripId, long expenseId) throws IOException {
		String baseDir = properties.getStorage().getReceiptsDir();
		Path dir = Path.of(baseDir, String.valueOf(tripId), String.valueOf(expenseId));
		Files.createDirectories(dir);
		String ext = extensionOf(file.getOriginalFilename());
		String filename = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
		Path target = dir.resolve(filename);
		try (InputStream in = file.getInputStream()) {
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		}
		String relative = String.join("/", String.valueOf(tripId), String.valueOf(expenseId), filename);
		String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
		return new StoredReceipt(relative, contentType, file.getOriginalFilename() != null ? file.getOriginalFilename() : "receipt");
	}

	public Path resolvePath(String relativePath) {
		return Path.of(properties.getStorage().getReceiptsDir()).resolve(relativePath);
	}

	private static String extensionOf(String original) {
		if (original == null || !original.contains(".")) {
			return "";
		}
		return original.substring(original.lastIndexOf('.') + 1).toLowerCase();
	}
}
