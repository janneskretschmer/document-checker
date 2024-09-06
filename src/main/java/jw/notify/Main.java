package jw.notify;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

public class Main {
	// private static final Properties PROPERTIES = new Properties();

	private static final String NO_NEW_DOCUMENTS_MESSAGE = "Seit der letzten Überprüfung aktualisierte Dokumente:\n"
			+ "Keine. Es sind keine Dokumente hinzugefügt oder geändert worden.";

	WebDriver driver;

	public static void main(String[] args)

			throws InterruptedException, IOException, AddressException, MessagingException {
		// PROPERTIES.load(new FileInputStream("jw-notify.properties"));
		WebDriver driver = null;
		try {
			Main main = new Main();
			driver = main.driver;
			AtomicBoolean success = new AtomicBoolean(true);
			main.getDocumentsIfUnread().ifPresent(updatedDocuments -> {
				/*
				 * try { sendMail(System.getenv("MAIL_RECIPIENT"),
				 * System.getenv("MAIL_SUBJECT"), System.getenv("MAIL_PREFIX") +
				 * StringEscapeUtils.escapeHtml4(updatedDocuments)); } catch (MessagingException
				 * e) { success.set(false); e.printStackTrace(); }
				 */
			});
			if (success.get()) {
				new URL(System.getenv("HEALTHCHECK")).getContent();
			}
		} catch (Exception e) {
			e.printStackTrace();

			if (driver == null) {
				System.out.println(
						"IF DRIVER IS OUTDATED: upgrade this package - https://mvnrepository.com/artifact/io.github.bonigarcia/webdrivermanager");
			}

			String failingPage = driver.getPageSource();
			System.out.println(failingPage);
			IOUtils.write(failingPage, new FileOutputStream("fail.html"), "UTF-8");
			/*
			 * sendMail(System.getenv("MAIL_RECIPIENT"), "Fehler in JW-Notify",
			 * "Sorry, heute hat es leider nicht geklappt. Bitte schau selber mal auf JW.org nach Neuerungen.<br /><br />"
			 * + e.getClass() + ": " + e.getMessage());
			 */
			if (System.getenv("SET_URL") != null) {
				IOUtils.toString(new URL(StringUtils.join(System.getenv("SET_URL"), URLEncoder.encode(
						"Sorry, heute hat es leider nicht geklappt. Bitte schau selber mal auf JW.org nach Neuerungen.<br /><br />"
								+ e.getClass() + ": " + e.getMessage(),
						"UTF-8"))).openStream(), "UTF-8");
			}
		}
	}

	/*
	 * !!! TLS-ERROR IN GITHUB-ACTIONS !!! => mails get sent in PHP
	 * 
	 * 
	 * public static void sendMail(String recipient, String subject, String text)
	 * throws AddressException, MessagingException { Properties prop = new
	 * Properties(); prop.put("mail.smtp.auth", true);
	 * prop.put("mail.smtp.starttls.enable", "true"); prop.put("mail.smtp.host",
	 * System.getenv("MAIL_SMTP_HOST")); prop.put("mail.smtp.port",
	 * System.getenv("MAIL_SMTP_PORT")); prop.put("mail.smtp.ssl.trust",
	 * System.getenv("MAIL_SMTP_HOST"));
	 * 
	 * Session session = Session.getInstance(prop, new Authenticator() {
	 * 
	 * @Override protected PasswordAuthentication getPasswordAuthentication() {
	 * return new PasswordAuthentication(System.getenv("MAIL_SENDER"),
	 * System.getenv("MAIL_SMTP_PASSWORD")); } });
	 * 
	 * Arrays.stream(recipient.split(",")).forEach(email -> { Message message = new
	 * MimeMessage(session); try { message.setFrom(new
	 * InternetAddress(System.getenv("MAIL_SENDER")));
	 * message.setRecipients(Message.RecipientType.TO,
	 * InternetAddress.parse(recipient)); message.setSubject(subject);
	 * 
	 * MimeBodyPart mimeBodyPart = new MimeBodyPart(); mimeBodyPart.setContent(text,
	 * "text/html");
	 * 
	 * Multipart multipart = new MimeMultipart();
	 * multipart.addBodyPart(mimeBodyPart);
	 * 
	 * message.setContent(multipart);
	 * 
	 * Transport.send(message); } catch (MessagingException e) {
	 * e.printStackTrace(); } }); }
	 */
	public Main() throws IOException {
//		URL resource = Main.class.getResource("/resources/geckodriver");
//		File f = new File("Driver");
//		if (!f.exists()) {
//			f.mkdirs();
//		}
//		File firefoxDriver = new File("Driver" + File.separator + "geckodriver");
//		if (!firefoxDriver.exists() || !firefoxDriver.canExecute()) {
//			firefoxDriver.createNewFile();
//			FileUtils.copyURLToFile(resource, firefoxDriver);
//			firefoxDriver.setExecutable(true);
//		}
//
//		System.setProperty("webdriver.gecko.driver", firefoxDriver.getAbsolutePath());
		WebDriverManager.chromedriver().setup();

		ChromeOptions options = new ChromeOptions();
		options.addArguments("--headless");
		options.addArguments("--window-size=1920,1080");
		driver = new ChromeDriver(options);
	}

	public Optional<String> getDocumentsIfUnread() throws IOException {
		String updatedDocuments = getUpdatedDocuments();
		if (NO_NEW_DOCUMENTS_MESSAGE.equals(updatedDocuments)
				|| IOUtils.toString(new URL(System.getenv("GET_URL")).openStream(), "UTF-8").equals(updatedDocuments)) {
			return Optional.empty();
		}
		if (!Boolean.parseBoolean(IOUtils.toString(
				new URL(StringUtils.join(System.getenv("SET_URL"), URLEncoder.encode(updatedDocuments, "UTF-8")))
						.openStream(),
				"UTF-8"))) {
			throw new RuntimeException("Writing old document state failed!");
		}
		;
		return Optional.of(updatedDocuments);
	}

	private String getUpdatedDocuments() {
		driver.get(System.getenv("URL"));

		// Cookie-Dialog
		// getElementWhenClickable(By.className("legal-notices-client--accept-button")).click();

		getElementWhenClickable(By.id("username")).sendKeys(System.getenv("USERNAME"));
		getElementWhenClickable(By.cssSelector("button[type=submit]")).click();

		getElementWhenClickable(By.id("passwordInput")).sendKeys(System.getenv("PASSWORD"));
		getElementWhenClickable(By.id("submitButton")).click();

		getElementWhenClickable(By.className("field__input--password")).sendKeys(System.getenv("ANSWER1"));
		driver.findElements(By.className("field__input--password")).get(1).sendKeys(System.getenv("ANSWER2"));
		// Overlay => js solution
		// getElementWhenClickable(By.cssSelector("button[type=submit]")).click();
		JavascriptExecutor ex = (JavascriptExecutor) driver;
		ex.executeScript("arguments[0].click();", getElementWhenClickable(By.cssSelector("button[type=submit]")));

		try {
			getElementWhenClickable(By.className("legal-notices-client--accept-button")).click();
		} catch (TimeoutException t) {
			System.out.println("No legal notice popup");
		}

		try {
			// Radio somehow not clickable => js solution
			ex.executeScript("arguments[0].checked = true;",
					getElementWhen(ExpectedConditions.presenceOfElementLocated(By.id("methodChoice-radio-item-2"))));
			getElementWhenClickable(By.cssSelector("button[type=submit]")).click();
		} catch (TimeoutException t) {
			System.out.println("No MFA survey");
		}

		// Overlay => js solution
		ex.executeScript("arguments[0].click();",
				getElementWhenClickable(By.xpath("//*[contains(text(), 'Documents')]")));

		getElementWhen(
				ExpectedConditions.and(ExpectedConditions.visibilityOfElementLocated(By.tagName("unread-documents")),
						(ExpectedConditions.invisibilityOfElementLocated(By.className("loading")))));

		String unreadDocuments = getElementWhen(
				ExpectedConditions.visibilityOfElementLocated(By.tagName("unread-documents"))).getText();

		driver.close();

		System.out.println(unreadDocuments);

		return unreadDocuments;
	}

	private WebElement getElementWhenClickable(By by) {
		return getElementWhen(ExpectedConditions.elementToBeClickable(by));
	}

	private <T> T getElementWhen(ExpectedCondition<T> elementToBeClickable) {
		return new WebDriverWait(driver, Duration.ofSeconds(30)).until(elementToBeClickable);
	}
}
