package club.axinyue.easydeal.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
    @GetMapping({
            "/room/{roomId}",
            "/room/{roomId}/{inviteCode}",
            "/about",
            "/stomp-test"
    })
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
