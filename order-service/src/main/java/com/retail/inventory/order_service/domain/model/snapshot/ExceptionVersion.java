package com.retail.inventory.order_service.domain.model.snapshot;

public enum ExceptionVersion {
    SERVICE_FAILURE(-1L),
    LOCAL_CACHE_FAILURE(-2L);

    private final Long version;

    ExceptionVersion(Long version) {
        this.version = version;
    }

    public Long getVersion() {
        return version;
    }
}
