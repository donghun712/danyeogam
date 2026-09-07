package com.danyeogam.backend.sync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.tour-sync")
public class TourSyncProperties {

    private boolean enabled;
    private String areaCode;
    private int pageSize = 50;
    private int maxPages = 1;
    private boolean hydrateDetails = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getAreaCode() { return areaCode; }
    public void setAreaCode(String areaCode) { this.areaCode = areaCode; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public int getMaxPages() { return maxPages; }
    public void setMaxPages(int maxPages) { this.maxPages = maxPages; }
    public boolean isHydrateDetails() { return hydrateDetails; }
    public void setHydrateDetails(boolean hydrateDetails) { this.hydrateDetails = hydrateDetails; }
}
