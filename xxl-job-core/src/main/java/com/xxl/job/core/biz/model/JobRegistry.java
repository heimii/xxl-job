package com.xxl.job.core.biz.model;

import com.xxl.job.core.handler.JobInfo;

import java.util.List;

/**
 * @author zhong
 * @date 2020-2-17
 */
public class JobRegistry {
    private String registryGroup;
    private String appName;
    private String address;
    private List<JobInfo> jobs;

    public String getRegistryGroup() {
        return registryGroup;
    }

    public void setRegistryGroup(String registryGroup) {
        this.registryGroup = registryGroup;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<JobInfo> getJobs() {
        return jobs;
    }

    public void setJobs(List<JobInfo> jobs) {
        this.jobs = jobs;
    }
}
