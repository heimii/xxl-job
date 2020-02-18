package com.xxl.job.core.thread;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.JobRegistry;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.RegistryConfig;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.JobInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by xuxueli on 17/3/2.
 */
public class ExecutorRegistryThread {
    private static Logger logger = LoggerFactory.getLogger(ExecutorRegistryThread.class);

    private static ExecutorRegistryThread instance = new ExecutorRegistryThread();
    private List<JobInfo> jobs;

    public static ExecutorRegistryThread getInstance() {
        return instance;
    }

    private Thread registryThread;
    private volatile boolean toStop = false;

    public void start(final String appName, final String address) {
        if (appName == null || appName.trim().length() == 0) {
            logger.warn(">>>>>>>>>>> xxl-job, executor registry config fail, appName is null.");
            return;
        }
        if (XxlJobExecutor.getAdminBizList() == null) {
            logger.warn(">>>>>>>>>>> xxl-job, executor registry config fail, adminAddresses is null.");
            return;
        }
        registryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                doRegister(appName, address);
            }
        });
        registryThread.setDaemon(true);
        registryThread.setName("xxl-job, executor ExecutorRegistryThread");
        registryThread.start();
    }

    protected void doRegister(String appName, String address) {
        boolean isJobsRegistered = false;
        while (!toStop) {
            if (!isJobsRegistered) {
                isJobsRegistered = registerJobs(appName, address);
            }
            // registry
            register(appName, address);
            sleep();
        }

        // registry remove
        unRegister(appName, address);
        logger.info(">>>>>>>>>>> xxl-job, executor registry thread destory.");

    }

    private boolean registerJobs(String appName, String address) {
        if (CollectionUtils.isEmpty(jobs)) {
            logger.info("not jobs found");
            return true;
        }
        JobRegistry registry = new JobRegistry();
        registry.setRegistryGroup(RegistryConfig.RegistType.EXECUTOR.name());
        registry.setAppName(appName);
        registry.setAddress(address);
        registry.setJobs(jobs);

        for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
            try {
                ReturnT<String> registryResult = adminBiz.registerNode(registry);
                if (registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()) {
                    registryResult = ReturnT.SUCCESS;
                    logger.trace(">>>>>>>>>>> xxl-job register node success, registryResult:{}", registryResult);
                    return true;
                } else {
                    logger.info(">>>>>>>>>>> xxl-job registry fail, registryResult:{}", registryResult);
                }
            } catch (Exception e) {
                logger.info(">>>>>>>>>>> xxl-job registry error, registryParam:{}", registry, e);
            }
        }
        return false;
    }

    private void register(String appName, String address) {
        try {
            RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(),
                    appName, address);
            for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
                try {
                    ReturnT<String> registryResult = adminBiz.registry(registryParam);
                    if (registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()) {
                        registryResult = ReturnT.SUCCESS;
                        logger.trace(">>>>>>>>>>> xxl-job registry success, registryParam:{}, registryResult:{}",
                                new Object[]{registryParam, registryResult});
                        break;
                    } else {
                        logger.info(">>>>>>>>>>> xxl-job registry fail, registryParam:{}, registryResult:{}",
                                new Object[]{registryParam, registryResult});
                    }
                } catch (Exception e) {
                    logger.info(">>>>>>>>>>> xxl-job registry error, registryParam:{}", registryParam, e);
                }

            }
        } catch (Exception e) {
            if (!toStop) {
                logger.error(e.getMessage(), e);
            }

        }
    }

    private void sleep() {
        try {
            if (!toStop) {
                TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
            }
        } catch (InterruptedException e) {
            if (!toStop) {
                logger.warn(">>>>>>>>>>> xxl-job, executor registry thread interrupted, error msg:{}",
                        e.getMessage());
            }
        }
    }

    private void unRegister(String appName, String address) {
        try {
            RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appName,
                    address);
            for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
                try {
                    ReturnT<String> registryResult = adminBiz.registryRemove(registryParam);
                    if (registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()) {
                        registryResult = ReturnT.SUCCESS;
                        logger.info(
                                ">>>>>>>>>>> xxl-job registry-remove success, registryParam:{}, registryResult:{}",
                                new Object[]{registryParam, registryResult});
                        break;
                    } else {
                        logger.info(
                                ">>>>>>>>>>> xxl-job registry-remove fail, registryParam:{}, registryResult:{}",
                                new Object[]{registryParam, registryResult});
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.info(">>>>>>>>>>> xxl-job registry-remove error, registryParam:{}",
                                registryParam, e);
                    }

                }

            }
        } catch (Exception e) {
            if (!toStop) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void toStop() {
        toStop = true;
        // interrupt and wait
        registryThread.interrupt();
        try {
            registryThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }


    public void setJobs(List<JobInfo> jobs) {
        this.jobs = jobs;
    }

    public List<JobInfo> getJobs() {
        return jobs;
    }
}
