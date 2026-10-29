package com.example.elibrary.domain.model;

import lombok.Getter;

/**
 * 书架上的一本实物，用条码标识。
 *
 * <p>为什么要有副本这层、而不是只记个数？因为一笔借阅必须能追到具体哪一册，
 * 这样才对得上账：快账 {@code book.available_copies} 随时可以拿每册的实物状态核一遍
 * （见 {@code ReconciliationService}）。
 */
@Getter
public class BookCopy {

    private Long id;
    private Long bookId;
    private String barcode;
    private CopyStatus status;
    private Long version;

    private BookCopy() {
    }

    public BookCopy(Long bookId, String barcode) {
        this(null, bookId, barcode, CopyStatus.AVAILABLE, null);
    }

    private BookCopy(Long id, Long bookId, String barcode, CopyStatus status, Long version) {
        this.id = id;
        this.bookId = bookId;
        this.barcode = barcode;
        this.status = status;
        this.version = version;
    }

    public static BookCopy restore(Long id, Long bookId, String barcode, CopyStatus status, Long version) {
        return new BookCopy(id, bookId, barcode, status, version);
    }

    public void assignIdForNewPersistence(Long generatedId) {
        this.id = generatedId;
    }

    public void lend() {
        if (status != CopyStatus.AVAILABLE) {
            throw new IllegalStateException("Copy " + barcode + " is not available: " + status);
        }
        status = CopyStatus.ON_LOAN;
    }

    public void takeBack() {
        if (status != CopyStatus.ON_LOAN) {
            throw new IllegalStateException("Copy " + barcode + " is not on loan: " + status);
        }
        status = CopyStatus.AVAILABLE;
    }

    public boolean isAvailable() {
        return status == CopyStatus.AVAILABLE;
    }
}
