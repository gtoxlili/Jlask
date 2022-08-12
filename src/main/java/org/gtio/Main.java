package org.gtio;

import com.alibaba.fastjson2.JSONObject;
import org.gtio.jlask.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) throws IOException {
        Jlask jlask = new Jlask("127.0.0.1", 8080, new Main());
        jlask.start();
    }

    @AssetsPath
    public String assets = "/assets";

    @Route(url = "/web")
    public Response web(Request req) throws IOException {
        return Response.RenderTemplate("/index.html");
    }

    @Route(url = "/")
    public Response index(Request req) {
        return new Response("<h1>Welcome to Jlask Server</h1>");
    }

    @Route(url = "/showJson")
    public Response showJson(Request req) {
        JSONObject json = new JSONObject();
        json.putAll(req.getParams());
        return new Response(json.toJSONString(), "application/json");
    }

    @Route(url = "/postJsonExample", method = "POST")
    public Response postJsonExample(Request req) {
        return new Response(req.getJsonBody().toJSONString(), "application/json");
    }

    @Route(url = "/postFromExample", method = "POST")
    public Response postFromExample(Request req) {
        return new Response(req.getFromBody().toString(), "text/plain");
    }

    @Route(url = "/setCookie")
    public Response setCookie(Request req) {
        Response res = new Response("set cookie success");
        res.SetCookie("name", req.getParams().get("name"));
        return res;
    }

    @Route(url = "/getCookie")
    public Response getCookie(Request req) {
        String cookie = req.getHeaders().get("Cookie");
        if (cookie == null) {
            return new Response("no cookie");
        }
        String name = cookie.split(";")[0].split("=")[1];
        // utf-8 encode
        name = new String(name.getBytes(), StandardCharsets.UTF_8);
        return new Response(name);
    }

    @Route(url = "/403")
    public Response forbidden(Request req) {
        // 程序默认定义了400, 403, 404, 500,501等错误码的页面，如果想自定义返回错误码，可以使用Response.ErrorStatus(int status, String body)方法
        return Response.ErrorStatus(ErrorType.Err_403);
    }

    @ErrorHandler(403)
    public Response notFound(Request req) {
        return Response.Redirect("/");
    }

    // 对于已经写了ErrorHandler注解的错误码，是无法返回默认页面的
    @Route(url = "/404")
    public Response invalidMethod(Request req) {
        return Response.ErrorStatus(ErrorType.Err_404);
    }

    // 可通过 method 来指定请求方法，如果没有指定，默认为 GET
    @Route(url = "/allMethod", method = {"GET", "POST"})
    public Response allMethod(Request req) {
        return new Response("<h1>这是一个可GET可POST的请求</h1>");
    }

    @Route(url = "/getVerification")
    public Response getVerification(Request req) throws IOException {

        BufferedImage img = new BufferedImage(75, 35, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 75, 35);
        g.setColor(Color.BLUE);
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString("GTIO", 0, 30);

        ByteArrayOutputStream verification = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", verification);
        return new Response(verification.toByteArray(), "image/jpeg", true, "南大.jpg");
    }

    @Route(url = "/uploadImg", method = "POST")
    public Response upload(Request req) {
        ArrayList<formData> formDatas = req.getFormDataBody();
        for (formData formData : formDatas) {
            if (formData.binaryValue != null) {
                return new Response(formData.binaryValue, formData.contentType);
            }
        }
        return null;
    }
}