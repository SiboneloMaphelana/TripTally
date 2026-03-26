package com.triptally.service.storage;

import lombok.Value;

@Value
public class StoredReceipt {

	String relativePath;
	String contentType;
	String originalFilename;
}
