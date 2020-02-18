package com.xxl.job.core.executor.impl;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.glue.GlueFactory;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.JobInfo;
import com.xxl.job.core.handler.annotation.JobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.handler.impl.MethodJobHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * xxl-job executor (for spring)
 *
 * @author xuxueli 2018-11-01 09:24:52
 */
public class XxlJobSpringExecutor extends XxlJobExecutor
        implements ApplicationContextAware, InitializingBean, DisposableBean {

    // start
    @Override
    public void afterPropertiesSet() throws Exception {

        // init JobHandler Repository
        initJobHandlerRepository(applicationContext);

        // init JobHandler Repository (for method)
        initJobHandlerMethodRepository(applicationContext);

        // refresh GlueFactory
        GlueFactory.refreshInstance(1);

        // super start
        super.start();
    }

    // destroy
    @Override
    public void destroy() {
        super.destroy();
    }


    private void initJobHandlerRepository(ApplicationContext applicationContext) {
        if (applicationContext == null) {
            return;
        }

        // init job handler action
        Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(JobHandler.class);

        if (serviceBeanMap != null && serviceBeanMap.size() > 0) {
            for (Object serviceBean : serviceBeanMap.values()) {
                if (serviceBean instanceof IJobHandler) {
                    String name = serviceBean.getClass().getAnnotation(JobHandler.class).value();
                    IJobHandler handler = (IJobHandler) serviceBean;
                    if (loadJobHandler(name) != null) {
                        throw new RuntimeException("xxl-job jobhandler[" + name + "] naming conflicts.");
                    }
                    registJobHandler(name, handler, null);
                }
            }
        }
    }

    private void initJobHandlerMethodRepository(ApplicationContext applicationContext) {
        if (applicationContext == null) {
            return;
        }

        // init job handler from method
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        if (beanDefinitionNames != null && beanDefinitionNames.length > 0) {
            for (String beanDefinitionName : beanDefinitionNames) {
                Object bean = applicationContext.getBean(beanDefinitionName);
                Method[] methods = bean.getClass().getDeclaredMethods();
                for (Method method : methods) {
                    XxlJob xxlJob = AnnotationUtils.findAnnotation(method, XxlJob.class);
                    if (xxlJob == null) {
                        continue;
                    }
                    addJobMethod(bean, method, xxlJob);

                }
            }
        }
    }
    
    private void addJobMethod(Object bean, Method method, XxlJob xxlJob) {
        // name
        String name = xxlJob.value();
        validateJobName(method, name);
        validateJobMethod(method);
        method.setAccessible(true);

        // init and destory
        Method initMethod = getInitMethod(method, xxlJob);
        Method destroyMethod = getDestroyMethod(method, xxlJob);

        // registry jobhandler
        JobInfo job = new JobInfo();
        job.setName(name);
        job.setTitle(xxlJob.title());
        job.setInitCron(xxlJob.cron());
        registJobHandler(name, new MethodJobHandler(bean, method, initMethod, destroyMethod), job);
    }

    private Method getInitMethod(Method method, XxlJob xxlJob) {
        Method initMethod = null;
        if (xxlJob.init().trim().length() > 0) {
            try {
                initMethod = method.getDeclaringClass().getDeclaredMethod(xxlJob.init());
                initMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(
                        "xxl-job method-jobhandler initMethod invalid, for[" + method.getDeclaringClass() + "#" +
                                method.getName() + "] .");
            }
        }
        return initMethod;
    }

    private Method getDestroyMethod(Method method, XxlJob xxlJob) {
        Method destroyMethod = null;
        if (xxlJob.destroy().trim().length() > 0) {
            try {
                destroyMethod = method.getDeclaringClass().getDeclaredMethod(xxlJob.destroy());
                destroyMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(
                        "xxl-job method-jobhandler destroyMethod invalid, for[" + method.getDeclaringClass() + "#" +
                                method.getName() + "] .");
            }
        }
        return destroyMethod;
    }

    private void validateJobName(Method method, String name) {
        if (name.trim().length() == 0) {
            throw new RuntimeException(
                    "xxl-job method-jobhandler name invalid, for[" + method.getDeclaringClass() + "#" +
                            method.getName() + "] .");
        }
        if (loadJobHandler(name) != null) {
            throw new RuntimeException("xxl-job jobhandler[" + name + "] naming conflicts.");
        }
    }

    private void validateJobMethod(Method method) {
        // execute method
        if (!(method.getParameterTypes() != null && method.getParameterTypes().length == 1 &&
                method.getParameterTypes()[0].isAssignableFrom(String.class))) {
            throw new RuntimeException(
                    "xxl-job method-jobhandler param-classtype invalid, for[" + method.getDeclaringClass() +
                            "#" + method.getName() + "] , " +
                            "The correct method format like \" public ReturnT<String> execute(String param) \" .");
        }
        if (!method.getReturnType().isAssignableFrom(ReturnT.class)) {
            throw new RuntimeException(
                    "xxl-job method-jobhandler return-classtype invalid, for[" + method.getDeclaringClass() + "#" +
                            method.getName() + "] , " +
                            "The correct method format like \" public ReturnT<String> execute(String param) \" .");
        }
    }

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

}
