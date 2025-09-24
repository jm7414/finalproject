package lx.project.dementia_care;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
	public String redirectToLogin() {
		return "redirect:/login";
	}
}


