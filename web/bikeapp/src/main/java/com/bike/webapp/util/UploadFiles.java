package com.bike.webapp.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import com.bike.webapp.exception.InvalidUploadException;

/**
 * Turns a client-supplied upload name into one that is safe to join onto a storage directory.
 * The client controls getOriginalFilename(), so it may carry "../", drive letters or an executable extension.
 */
public final class UploadFiles {

	private static final Set<String> ALLOWED_EXTENSIONS =
			new HashSet<String>(Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp", "gpx"));
	private static final int MAX_NAME_LENGTH = 100;

	private UploadFiles() {
	}

	/**
	 * Returns the base name with unsafe characters replaced, or "" when no file was sent.
	 * Throws InvalidUploadException (HTTP 400) when the extension is not an allowed image/GPX type.
	 */
	public static String safeName(MultipartFile file) {
		String original = file == null ? null : file.getOriginalFilename();
		if (original == null || original.trim().isEmpty()) {
			return "";
		}
		String name = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1);
		name = name.replaceAll("[^\\p{L}\\p{N}._-]", "_").replaceAll("^\\.+", "");

		int dot = name.lastIndexOf('.');
		String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
		if (name.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
			throw new InvalidUploadException("Unsupported upload file type");
		}
		if (name.length() > MAX_NAME_LENGTH) {
			name = name.substring(0, MAX_NAME_LENGTH - extension.length() - 1) + "." + extension;
		}
		return name;
	}
}
