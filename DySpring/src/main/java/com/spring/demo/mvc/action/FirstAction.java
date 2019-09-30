package com.spring.demo.mvc.action;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.spring.demo.service.INamedService;
import com.spring.demo.service.IService;
import com.spring.framework.annotation.Autowired;
import com.spring.framework.annotation.Controller;
import com.spring.framework.annotation.RequestMapping;
import com.spring.framework.annotation.RequestParam;
import com.spring.framework.annotation.ResponseBody;
import com.spring.framework.servlet.ModelAndView;


@Controller
@RequestMapping("/web")
public class FirstAction {
	@Autowired private IService service;
	
	@Autowired private INamedService namedService;
	
	@RequestMapping("/query/.*.json")
	public ModelAndView query(HttpServletRequest request,HttpServletResponse response,@RequestParam("name") String name) {
//			response.getWriter().write("get params name = " + name);
//		out(response, "get params name = " + name);
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("name", name);
		return new ModelAndView("first.dyml",model);
	}
	
	@RequestMapping("/query/.*.json")
	@ResponseBody
	public ModelAndView add(HttpServletRequest request,HttpServletResponse response,@RequestParam("name") String name) {
		out(response, "this is json");
		return null;
	}
	
	public void out(HttpServletResponse response,String str) {
		try {
			response.getWriter().write(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
