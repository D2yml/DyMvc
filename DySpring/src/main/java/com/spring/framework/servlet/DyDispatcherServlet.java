package com.spring.framework.servlet;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.spring.framework.annotation.Controller;
import com.spring.framework.annotation.RequestMapping;
import com.spring.framework.annotation.RequestParam;
import com.spring.framework.context.DyapplicationContext;

public class DyDispatcherServlet extends HttpServlet{

	private static final long serialVersionUID = 1L;
	private static final String LOCATION = "contextConfigLocation";
	
//	private Map<Pattern,Handler> handlerMapping = new HashMap<Pattern, Handler>();
	
	private List<Handler> handlerMapping = new ArrayList<Handler>();
	
	private Map<Handler, HandlerAdapter> adapterMapping = new HashMap<Handler, HandlerAdapter>();
	
	private List<ViewResolver> viewResolvers = new ArrayList<ViewResolver>();
	
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.service(req, resp);
	}

	//初始化IOC容器
	@Override
	public void init(ServletConfig config) throws ServletException {
		//IOC必须要容器初始化
		DyapplicationContext context = new DyapplicationContext(config.getInitParameter(LOCATION));
		// 请求解析
		initMultipartResolver(context);
		// 国际化
		initLocaleResolver(context);
		// view主题
		initThemeResolver(context);
		// 解析url和Method关系(HandlerMapping) URM(Url Relation Mapping)
		initHandlerMappings(context);
		// 适配器(匹配过程)
		initHandlerAdapters(context);
		// 异常解析
		initHandlerExceptionResolvers(context);
		// 视图转发(根据视图名匹配到具体模板)
		initRequestToViewNameTranslator(context);
		// 解析模板中内容(那到服务器数据生成HTML)
		initViewResolvers(context);

		initFlashMapManager(context);
		System.out.println("DyMvc init over");
	}

	private void initFlashMapManager(DyapplicationContext context) {
		
	}

	private void initViewResolvers(DyapplicationContext context) {
		//模板一般是不会放在webRoot下,而是WEB-INF下或者classes下避免用户直接访问
		//加载模板个数存储到缓存中
		//检查模板中错误的语法
		String templateRoot = context.getConfig().getProperty("templateRoot");
		//归根到底就是一个普通文件
		String rootPath = this.getClass().getClassLoader().getResource(templateRoot).getFile();
		File rootDir = new File(rootPath);
		for (File template : rootDir.listFiles()) {
			viewResolvers.add(new ViewResolver(template.getName(),template));
		}
	}

	private void initRequestToViewNameTranslator(DyapplicationContext context) {
		
	}

	private void initHandlerExceptionResolvers(DyapplicationContext context) {
		
	}

	//动态匹配参数
	//动态赋值
	private void initHandlerAdapters(DyapplicationContext context) {
		if (handlerMapping.isEmpty()) {
			return;
		}
		//参数类型为Key,参数索引号位Value
		Map<String,Integer> paramMapping = new HashMap<String, Integer>(); 
		//这里只需要具体的某个方法
		for (Handler handler : handlerMapping) {
			//将方法上所有参数获取
			Class<?>[] parameterTypes = handler.method.getParameterTypes();
			//匹配自定义参数列表
			for (int i = 0; i < parameterTypes.length; i++) {
				Class<?> type = parameterTypes[i];
				if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
					paramMapping.put(type.getName(), i);
				}
			}
			
			//匹配request和Response
			Annotation[][] pa = handler.method.getParameterAnnotations();
			for (int i = 0; i < pa.length; i++) {
				for (Annotation a : pa[i]) {
					if (a instanceof RequestParam) {
						String paramName = ((RequestParam) a).value();
						if (!"".equals(paramName.trim())) {
							paramMapping.put(paramName, i);
						}
					}
				}
			}
			adapterMapping.put(handler, new HandlerAdapter(paramMapping));
		}
	}

	private void initHandlerMappings(DyapplicationContext context) {
		//工厂方法
		Map<String, Object> ioc = context.getAll();
		if (ioc.isEmpty()) {
			return;
		}
		for (Entry<String, Object> entry : ioc.entrySet()) {
			Class<? extends Object> clazz = entry.getValue().getClass();
			if (!clazz.isAnnotationPresent(Controller.class)) {
				continue;
			}
			String url = "";
			//拼接类上的RequestMaping
			if (clazz.isAnnotationPresent(RequestMapping.class)) {
				RequestMapping mapping = clazz.getAnnotation(RequestMapping.class);
				url = mapping.value();
			}
			//扫描类中所有方法
			Method[] methods = clazz.getMethods();
			for (Method method : methods) {
				if (!method.isAnnotationPresent(RequestMapping.class)) {
					continue;
				}
				RequestMapping reqMapping = method.getAnnotation(RequestMapping.class);
				
				String regex = (url + reqMapping.value()).replaceAll("/+", "/");
				Pattern pattern = Pattern.compile(regex);
				
				//写入handlerMapping
				handlerMapping.add(new Handler(entry.getValue(),method, pattern));
//				handlerMapping.put(pattern, new Handler(entry.getValue(),method));
			}
		}
		//只要是由Controller修饰的类,里面的方法全部找出
		//而且这个方法上应该要加RequestMapping,如果没加这个注解,则不能被外界访问0
		
		//RequestMapping会配置一个url,那么一个url就对应一个方法,并将这个方法对应到Map中
	}

	private void initThemeResolver(DyapplicationContext context) {
		
	}

	private void initLocaleResolver(DyapplicationContext context) {
		
	}

	private void initMultipartResolver(DyapplicationContext context) {
		
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	//在这里调用自己写的Controller
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			doDispatch(req, resp);
		} catch (Exception e) {
			resp.getWriter().write("500 Exception Msg:" + Arrays.toString(e.getStackTrace()));
			e.printStackTrace();
		}
	}
	
	
	
	private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception{
		//先取出一个Handler,从HandlerMapping取
		Handler handler = getHandler(req);
		if (handler == null) {
			resp.getWriter().write("404 Not Found");
			return;
		}
		//在取出一个适配器
		HandlerAdapter ha = getHandlerAdapter(handler);
		//再有适配器去调用方法
		ModelAndView mv = ha.handle(req, resp, handler);
		applyDefaultViewName(resp,mv);
		//自定义模板框架
		
	}
	
	private void applyDefaultViewName(HttpServletResponse resp, ModelAndView mv) throws Exception {
		if (mv == null) {
			return;
		}
		if (viewResolvers.isEmpty()) {
			return;
		}
		for (ViewResolver resolver : viewResolvers) {
			if (mv.getView().equals(resolver.getViewName())) {
				continue;
			}
			String r = resolver.parse(mv);
			if (r !=  null) {
				resp.getWriter().write(r);
				break;
			}
		}
	}

	private Handler getHandler(HttpServletRequest req) {
		if (handlerMapping.isEmpty()) {
			return null;
		}
		String url = req.getRequestURI();
		String contextPath = req.getContextPath();
		url = url.replace(contextPath, "").replaceAll("/+", "/");
		for (Handler entry : handlerMapping) {
			Matcher matcher = entry.pattern.matcher(url);
			if (!matcher.matches()) {
				continue;
			}
			return entry;
		}
		return null;
	}
	
	private HandlerAdapter getHandlerAdapter(Handler handler) {
		if (adapterMapping.isEmpty()) {
			return null;
		}
		return adapterMapping.get(handler);
	}
	
	/**
	 * handlerMapping定义
	 * @author Administrator
	 *
	 */
	private class Handler{
		protected Object controller;
		protected Method method;
		protected Pattern pattern;
		
		protected Handler(Object controller,Method method,Pattern pattern) {
			this.controller = controller;
			this.method = method;
			this.pattern = pattern;
		}
	}
	
	/**
	 * 方法适配器
	 * @author Administrator
	 *
	 */
 	private class HandlerAdapter{
		
		private Map<String, Integer> paramMapping;
		public HandlerAdapter(Map<String, Integer> paramMapping) {
			this.paramMapping = paramMapping;
		}

		//主要摩的诗用反射调用url对应的method
		@SuppressWarnings("unchecked")
		public ModelAndView handle(HttpServletRequest req, HttpServletResponse resp,Handler handler) throws Exception{
			//为什么要传Request,为什么要传Response,为什么要传handler
			//Controller中request和Response是要赋值的
			//拿到handler就能拿到method
			Class<?>[] parameterTypes = handler.method.getParameterTypes();
			//要想给参数赋值只能通过索引来找到具体的某个参数
			Object[] paramVlaues = new Object[parameterTypes.length];
			Map<String,String[]> params = req.getParameterMap();
			for (Entry<String, String[]> param : params.entrySet()) {
				String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
				if (!this.paramMapping.containsKey(param.getKey())) {
					continue;
				}
				int index = this.paramMapping.get(param.getKey());
				//单个赋值是不行的
				paramVlaues[index] = castStringValue(value, parameterTypes[index]);
			}
			//request和response赋值
			String reqName = HttpServletRequest.class.getName();
			if (this.paramMapping.containsKey(reqName)) {
				Integer reqIndex = this.paramMapping.get(HttpServletRequest.class.getName());
				paramVlaues[reqIndex] = req;
			}
			String respName = HttpServletResponse.class.getName();
			if (this.paramMapping.containsKey(respName)) {
				Integer respIndex = this.paramMapping.get(HttpServletResponse.class.getName());
				paramVlaues[respIndex] = resp;
			}
			boolean isModelAndView = handler.method.getReturnType() == ModelAndView.class;
			Object r = handler.method.invoke(handler.controller, paramVlaues);
			if (isModelAndView) {
				return (ModelAndView)r;
			}
			return null;
		}
		private Object castStringValue(String value,Class<?> clazz) {
			if (clazz == String.class) {
				return value;
			} else if (clazz == Integer.class){
				Integer.valueOf(value);
			} else if (clazz == int.class) {
				Integer.valueOf(value).intValue();
			} else {
				return null;
			}
			return null;
		}
	}
	
 	private class ViewResolver{
 		private String viewName;
 		private File file;
 		
 		protected ViewResolver(String viewName,File file) {
			this.viewName = viewName;
			this.file = file;
		}
 		
 		protected String parse(ModelAndView mv) throws Exception {
 			StringBuffer sb = new StringBuffer();
 			RandomAccessFile ra = new RandomAccessFile(this.file, "r");
 			//模板框架的语法是非常复杂的,但是原理是一样的
 			//都是用正则表达式来处理字符串而已
 			try {
	 			String line = null;
	 			while (null != (line = ra.readLine())) {
	 				sb.append(line);
	 				Matcher m = match(line);
	 				while (m.find()) {
	 					for (int i = 1; i < m.groupCount(); i++) {
							String paramName = m.group();
							Object paramValue = mv.getModel().get(paramName);
							if (null == paramValue) {
								continue;
							}
							line = line.replaceAll("@\\{" + paramName + "\\}", paramValue.toString());
	 					}
	 				}
				}
 			} finally {
 				ra.close();
 			}
			return sb.toString();
		}

 		private Matcher match(String str) {
 			Pattern pattern = Pattern.compile("@\\{(.+?)\\}",Pattern.CASE_INSENSITIVE);
 			Matcher m = pattern.matcher(str);
 			return m;
 		}
 		
		public String getViewName() {
			return viewName;
		}

 		
 	}
}
