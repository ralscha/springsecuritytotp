package ch.rasc.twofactor.security;

import static ch.rasc.twofactor.db.Tables.APP_USER;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.jooq.DSLContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

@Controller
public class QRCodeController {

	private final DSLContext dsl;

	QRCodeController(DSLContext dsl) {
		this.dsl = dsl;
	}

	@RequestMapping(value = "/qrcode/{username}.png", method = RequestMethod.GET)
	public void qrcode(HttpServletResponse response,
			@PathVariable("username") String username)
			throws WriterException, IOException {

		var result = this.dsl.select(APP_USER.EMAIL, APP_USER.SECRET).from(APP_USER)
				.where(APP_USER.USER_NAME.eq(username)).limit(1).fetchOne();

		if (result != null) {
			response.setContentType("image/png");
			String contents = "otpauth://totp/" + username + ":"
					+ result.get(APP_USER.EMAIL) + "?secret="
					+ result.get(APP_USER.SECRET) + "&issuer=SpringSecurityTOTP";

			QRCodeWriter writer = new QRCodeWriter();
			BitMatrix matrix = writer.encode(contents, BarcodeFormat.QR_CODE, 200, 200);
			MatrixToImageWriter.writeToStream(matrix, "PNG", response.getOutputStream());
			response.getOutputStream().flush();
		}
	}

}
