package com.spring.framework.context;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.spring.framework.annotation.Autowired;
import com.spring.framework.annotation.Controller;
import com.spring.framework.annotation.Service;


public class DyapplicationContext {
	
	private Map<String, Object> instanceMapping = new ConcurrentHashMap<String, Object>();
	
	//类似于内部的配置信息,我们在外部是看不到的
	//我们能够看到的只有IOC容器 getBean方法来间接调用
	private List<String> classCache = new ArrayList<String>();
	
	private Properties config = new Properties();
	
	public DyapplicationContext(String location) {
		InputStream is = null;
		try {
			//定位
			is = this.getClass().getClassLoader().getResourceAsStream(location);
			//载入
			Properties config = new Properties();
			config.load(is);
			
			//注册,把所有Class找出保存
			String packageName = config.getProperty("scanPackage");
			doRegister(packageName);
			//初始化,只要循环Class
			doCreateBean();
			//注入
			populate();
		} catch (Exception e) {
			e.printStackTrace();
		}
		//加载配置
		//定位,载入,注册,初始化,注入
		System.out.println("IOC Container init over");
	}
	
	//把符合条件的所有Class注册到缓存
	private void doRegister(String packageName) {
		URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
		File dir = new File(url.getFile());
		for (File file : dir.listFiles()) {
			//如果是一个文件夹,继续递归
			if (file.isDirectory()) {
				doRegister(packageName + "." + file.getName());
			} else {
				classCache.add(packageName + "." + file.getName().replace(".class", "").trim());
			}
		}
	}

	private void doCreateBean() {
		//检查注册信息
		if (classCache.size() == 0) {
			return;
		}
		try {
			for (String className : classCache) {
				//Spring中会判断是JDK还是CGLib代理
				Class<?> clazz = Class.forName(className);
				//判断是否需要初始化
				//只要加了@Service @Controller要初始化
				if (clazz.isAnnotationPresent(Controller.class)) {
					String id = lowerFirstChar(clazz.getSimpleName());
					instanceMapping.put(id, clazz.newInstance());
				} else if(clazz.isAnnotationPresent(Service.class)) {
					Service service = clazz.getAnnotation(Service.class);
					//如果设置了自定义名,优先使用自定义名
					String id = service.value();
					if (!"".equals(id.trim())) {
						instanceMapping.put("id", clazz.newInstance());
						continue;
					}
					// 如果是空,使用默认规则
					// 1. 尅名首字母小写
					// 如果是接口
					// 2. 可以根据类型匹配
					Class<?>[] interfaces = clazz.getInterfaces();
					// 如果这个类实现了接口,就用接口类型作为key
					for (Class<?> i : interfaces) {
						instanceMapping.put(i.getName(), clazz.newInstance());
					}
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private String lowerFirstChar(String str) {
		char[] chars = str.toCharArray();
		//通过Ascii编码转小写(不考虑特殊字符,本身就是小写)
		chars[0] += 32;
		return String.valueOf(chars);
	}
	
	private void populate() throws IllegalArgumentException, IllegalAccessException  {
		//首先判断ioc容器中有没有东西
		if (instanceMapping.isEmpty()) {
			return;
		}
		for (Entry<String,Object> entry : instanceMapping.entrySet()) {
			//把所有属性全部取出,包括私有属性
			Field[] fields = entry.getValue().getClass().getDeclaredFields();
			for (Field field : fields) {
				if (!field.isAnnotationPresent(Autowired.class)) {
					continue;
				}
				Autowired autowired = field.getAnnotation(Autowired.class);
//				Autowired autowired = (Autowired) field.get(Autowired.class);
				String id = autowired.value().trim();
				//如果id为空,默认根据类型来注入
				if ("".equals(id)) {
					id = field.getType().getName();
				}
				//把私有变量开放访问权限
				field.setAccessible(true);
				
				try {
					field.set(entry.getValue(), instanceMapping.get(id));
				} catch (Exception e) {
					e.printStackTrace();
				} 
			}
		}
	}
	
//	public Object getBean(String name) {
//		return null;
//	}
	
	public Map<String, Object> getAll() {
		return instanceMapping;
	}

	public Properties getConfig() {
		return config;
	}

	
}
