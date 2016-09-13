package ch.rasc.sec.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import ch.rasc.sec.entity.User;
import ch.rasc.sec.repository.UserRepository;

@Controller
public class QRCodeController {

	private final UserRepository userRepository;

	QRCodeController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@RequestMapping(value = "/qrcode/{username}.png", method = RequestMethod.GET)
	public void qrcode(HttpServletResponse response,
			@PathVariable("username") String username)
			throws WriterException, IOException {

		User user = this.userRepository.findByUserName(username);
		if (user != null) {
			response.setContentType("image/png");
			String contents = "otpauth://totp/" + username + ":" + user.getEmail()
					+ "?secret=" + user.getSecret() + "&issuer=SpringSecurityTOTP";

			QRCodeWriter writer = new QRCodeWriter();
			BitMatrix matrix = writer.encode(contents, BarcodeFormat.QR_CODE, 200, 200);
			MatrixToImageWriter.writeToStream(matrix, "PNG", response.getOutputStream());
			response.getOutputStream().flush();
		}
	}

}
