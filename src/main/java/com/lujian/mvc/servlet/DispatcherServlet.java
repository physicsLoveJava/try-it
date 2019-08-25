package com.lujian.mvc.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lujian.mvc.annotation.Autowired;
import com.lujian.mvc.annotation.Controller;
import com.lujian.mvc.annotation.RequestMapping;
import com.lujian.mvc.annotation.RequestParam;
import com.lujian.mvc.annotation.Service;
import lombok.Data;

/**
 * @author lj167323@alibaba-inc.com
 */
public class DispatcherServlet extends HttpServlet {

    private Properties configContext = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> ioc = new HashMap<>();

    private List<Handler> handlerMapping = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatcher(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp)
        throws IOException, InvocationTargetException, IllegalAccessException {
        Handler handler = getHandler(req);
        if (handler == null) {
            resp.getWriter().write("404 Not Found");
            return;
        }
        Class<?>[] paramTypes = handler.getParamTypes();

        Object[] paramValues = new Object[paramTypes.length];

        Map<String, String[]> params = req.getParameterMap();

        for (Entry<String, String[]> param : params.entrySet()) {
            String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll("\\s", ",");
            if (!handler.paramIndexMapping.containsKey(param.getKey())) {
                continue;
            }
            Integer index = handler.paramIndexMapping.get(param.getKey());
            paramValues[index] = this.convert(paramTypes[index], value);
        }

        if (handler.paramIndexMapping.containsKey(HttpServletRequest.class.getName())) {
            Integer reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = req;
        }
        if (handler.paramIndexMapping.containsKey(HttpServletResponse.class.getName())) {
            Integer reqIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[reqIndex] = resp;
        }

        Object returnValue = handler.method.invoke(handler.controller, paramValues);
        if (returnValue == null || returnValue instanceof Void) {
            return;
        }
        resp.getWriter().write(returnValue.toString());
    }

    private Object convert(Class<?> paramType, String value) {
        if (Integer.class == paramType) {
            return Integer.valueOf(value);
        }
        return value;
    }

    private Handler getHandler(HttpServletRequest req) {
        if (handlerMapping == null) {
            return null;
        }

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");
        for (Handler handler : handlerMapping) {
           try {
                Matcher matcher = handler.pattern.matcher(url);
                if (!matcher.matches()) {
                    continue;
                }
                return handler;
           } catch (Exception e) {
               throw e;
           }
        }
        return null;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        doScanner(configContext.getProperty("scanPackage"));

        doInstance();

        doAutowired();

        initHandlerMapping();

        System.out.println("mvc framework is loaded");
    }

    private void initHandlerMapping() {
        for (Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(Controller.class)) {
                continue;
            }
            String baseUrl = "";
            if (clazz.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                baseUrl = requestMapping.value();
            }
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(RequestMapping.class)) {
                    continue;
                }
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                String regex = (baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
                handlerMapping.add(new Handler(entry.getValue(), method, Pattern.compile(regex)));
                System.out.println("Mapped " + regex + "," + method);
            }
        }
    }

    private void doAutowired() {
        try {
            for (Entry<String, Object> entry : ioc.entrySet()) {
                Field[] fields = entry.getValue().getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (!field.isAnnotationPresent(Autowired.class)) {
                        continue;
                    }
                    Autowired autowired = field.getAnnotation(Autowired.class);
                    String beanName = autowired.value().trim();
                    if ("".equals(beanName)) {
                        beanName = field.getType().getName();
                    }
                    field.setAccessible(true);
                    field.set(entry.getValue(), ioc.get(beanName));
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void doInstance() {
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                Object instance = clazz.newInstance();
                if (clazz.isAnnotationPresent(Controller.class)) {
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, instance);
                } else if (clazz.isAnnotationPresent(Service.class)) {
                    Service service = clazz.getAnnotation(Service.class);
                    String beanName = service.value();
                    if ("".equals(beanName.trim())) {
                        beanName = toLowerFirstCase(clazz.getName());
                    }
                    ioc.put(beanName, instance);
                    for (Class<?> anInterface : clazz.getInterfaces()) {
                        if (ioc.containsKey(anInterface.getName())) {
                            throw new RuntimeException("The " + anInterface.getName() + " exists");
                        }
                        ioc.put(anInterface.getName(), instance);
                    }
                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String toLowerFirstCase(String name) {
        char[] names = name.toCharArray();
        names[0] += 32;
        return String.valueOf(names);
    }

    private void doLoadConfig(String configPath) {
        InputStream is = null;
        try {
            is = this.getClass().getClassLoader().getResourceAsStream(configPath);
            configContext.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classDir = new File(url.getFile());
        for (File file : classDir.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                String clazzName = scanPackage + "." + file.getName().replace(".class", "");
                classNames.add(clazzName);
            }
        }
    }

    private class Handler {
        protected Object controller;
        protected Method method;
        protected Pattern pattern;
        protected Map<String, Integer> paramIndexMapping;

        public Handler() {
        }

        public Handler(Object controller, Method method, Pattern pattern) {
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;
            this.paramIndexMapping = new HashMap<>();
            putParamIndexMapping(method);
        }

        private void putParamIndexMapping(Method method) {
            Annotation[][] pa = method.getParameterAnnotations();
            for (int i = 0; i < pa.length; i++) {
                for (Annotation a : pa[i]) {
                    if (a instanceof RequestParam) {
                        String paramName = ((RequestParam)a).value();
                        if (!"".equals(paramName.trim())) {
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }
            }
            Class<?>[] paramsTypes = method.getParameterTypes();
            for (int i = 0; i < paramsTypes.length; i++) {
                Class<?> type = paramsTypes[i];
                if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                    paramIndexMapping.put(type.getName(), i);
                }
            }
        }

        public Class<?>[] getParamTypes() {
            return method.getParameterTypes();
        }
    }
}
