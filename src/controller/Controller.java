package controller;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Date;

public class Controller {

	Socket socket = null;
	BufferedReader reader = null;
	DataOutputStream writer = null;
	
	int operatorNumber = 0;
	String operatorName = null;
	int productNumber = 0;
	String productName = null;
	double tara = 0;
	double netto = 0;
	
	public Controller() {

		try {

			socket = new Socket("169.254.2.2", 8000);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new DataOutputStream(socket.getOutputStream());
			System.out.println("test" + reader.readLine());
			step1();

		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		} finally {
			try {
				socket.close();
			} catch (Exception e) {
			}
			try {
				reader.close();
			} catch (Exception e) {
			}
			try {
				writer.close();
			} catch (Exception e) {
			}
		}

	}

	private void step1() throws Exception {
		
		// Step 1. Identificer operatør.
		// -----------------------------
		// Send: RM20 4 "Operatør nummer:" "" ""
		// Modtag: RM20 B
		// Modtag: RM20 A # // # er den indtastede værdi.
		// Valider input og fortsæt til step 2.

		writer.writeBytes("RM20 4 \"Operatør nummer:\" \" \" \" \"\r\n");
		
		if (!reader.readLine().equals("RM20 B")) {
			step1error();
			return;
		}
		String response = RM20(reader.readLine());
		if (response == null || !response.matches("^[0-9]+$")) {
			step1error();
			return;
		}
		operatorNumber = Integer.parseInt(response);
		operatorName = getOperatorName(operatorNumber);
		if (operatorName == null) {
			step1error();
			return;
		}
		step2();

	}

	private void step1error() throws Exception {

		// Step 1. Fejlet.
		// ---------------
		// Send: D Ukendt operatør.
		// Modtag: D A
		// Vent 2 sekunder.
		// Gentag step 1.

		writer.writeBytes("D Ukendt operatør.\r\n");
		if (!reader.readLine().equals("D A")) {
			step1error();
			return;
		}
		Thread.sleep(2000);
		step1();

	}

	private void step2() throws Exception {
		
		// Step 2. Identificer vare.
		// -------------------------
		// Send: RM20 4 "Vare nummer:" "" ""
		// Modtag: RM20 B
		// Modtag: RM20 A # // # er den indtastede værdi.
		// Valider input og retuner til step 1 eller forsæt til step 3.
		
		writer.writeBytes("RM20 4 \"Vare nummer:\" \" \" \" \"\r\n");
		reader.readLine().equals("RM20 B");
		String response = RM20(reader.readLine());
		
		if (response == null || !response.matches("^[0-9]+$")){
			step2error();
			return;
		} else if (response.equals("0")) {
			step1();
			return;
		}
		productNumber = Integer.parseInt(response);
		productName = getProductName(productNumber);
		if(productName == null){
			step2error();
			return;
		}
		
		step3();
		
	}		

	private void step2error() throws Exception {
		
		// Step 2. Fejlet.
		// ---------------
		// Send: D Ukendt vare.
		// Modtag: D A
		// Vent 2 sekunder.
		// Gentag step 2.

		writer.writeBytes("D \"Ukendt\"\r\n");
		if (!reader.readLine().equals("D A")) {	
			step2error();
			return;
		}
		Thread.sleep(2000);
		step2();

	}

	private void step3() throws Exception {
		
		// Step 3. Bekræft vare.
		// ---------------------
		// Send:	RM20 4 "Korrekt vare?" "#" "1/0" // # er vare navnet.
		// Modtag:	RM20 B
		// Modtag:	RM20 A # // # er den indtastede værdi.
		// Valider input og retuner til step 2 eller fortsæt til step 4.
		
		writer.writeBytes("RM20 4 \"Korrekt vare? " + productName + "\" \"\" \"1/0\"\r\n");
		if (!reader.readLine().equals("RM20 B")) {	
			step3error();
			return;
		}
		String response = RM20(reader.readLine());
		if (response == null) {
			step3error();
			return;
		}
		if (response.equals("0")) {
			step2();
			return;
		} else if (response.equals("1")) {
			step4();
			return;
		} else {
			step3error();
			return;
		}
		
	}

	private void step3error() throws Exception {
		
		// Step 3. Fejlet.
		// ---------------
		// Send:	D Ugyldigt input.
		// Modtag:	D A
		// Vent 2 sekunder.
		// Gentag step 3.

		writer.writeBytes("D Ugyldigt input.\r\n");
		if (!reader.readLine().equals("D A")) {	
			step3error();
			return;
		}
		Thread.sleep(2000);
		step3();
		
	}
	
	private void step4() throws Exception {
		
		// Step 4. Tarer vægt.
		// -------------------
		// Send: RM20 4 "Placer skål på vægten." " " "1/0"
		// Modtag: RM20 B
		// Modtag: RM20 A # // # er den indtastede værdi.
		// Valider input og retuner til step 3 eller fortsæt.
		// Send: T
		// Modtag: T S # kg // # er den nye tara, punktum bruges som decimaltegn.
		
		writer.writeBytes("RM20 4 \"Placer skålen på vægten.\" \" \" \"1/0\"\r\n");	
		if (!reader.readLine().equals("RM20 B")) {
			step4error();
			return;
		}
		String response = RM20(reader.readLine());
		if (response == null) {
			step4error();
			return;
		} else if (response.equals("0")) {
			step3();
			return;
		} else if (!response.equals("1")) {
			step4error();
			return;
		}
		writer.writeBytes("T\r\n");
		response = T(reader.readLine());
		if (response == null) {
			step4error();
			return;
		}
		tara = Double.parseDouble(response);
		step5();		
		
	}

	private void step4error() throws Exception {
		
		// Step 4. Fejlet.
		// ---------------
		// Send: D Ugyldigt input.
		// Modtag: D A
		// Vent 2 sekunder.
		// Gentag step 4.
		
		writer.writeBytes("D Ugyldigt input.\r\n");
		if (!reader.readLine().equals("D A")) {
			step1error();
			return;
		}
		Thread.sleep(2000);
		step4();

	}

	private void step5() throws Exception {
		
		// Step 5. Afvej vare.
		// -------------------
		// Send: RM20 4 "Placer vare i skålen." " " "1/0"
		// Modtag: RM20 B
		// Modtag: RM20 A # // # er den indtastede værdi.
		// Send: S
		// Modtag: S S # kg // # er netto vægten, punktum bruges som decimaltegn.
		
		writer.writeBytes("RM20 4 \"Placer vare i skålen.\" \" \" \"1/0\"\r\n");
		if (!reader.readLine().equals("RM20 B")) {
			step5error();
			return;
		}
		String response = RM20(reader.readLine());
		if (response == null) {
			step5error();
			return;
		} else if (response.equals("0")) {
			step4();
			return;
		} else if (!response.equals("1")) {
			step5error();
			return;
		}
		writer.writeBytes("S\r\n");
		response = S(reader.readLine());
		if (response == null) {
			step5error();
			return;
		}
		netto = Double.parseDouble(response);
		step6();	

	}

	private void step5error() throws Exception{
		
		// Step 5. Fejlet.
		// ---------------
		// Send: D Ugyldigt input.
		// Modtag: D A
		// Vent 2 sekunder.
		// Gentag step 5.
		
		writer.writeBytes("D Ugyldigt input.\r\n");
		if (!reader.readLine().equals("D A")) {
			step5error();
			return;
		}
		Thread.sleep(2000);
		step5();

	}
	
	private void step6() throws Exception{

		// Step 6. Kontroller brutto vægt.
		// -------------------------------
		// Send:	RM20 4 "Ryd vægten." " " "1/0"
		// Modtag:	RM20 B
		// Modtag:	RM20 A # // # er den indtastede værdi.
		// Valider input og retuner til step 5 eller fortsæt.
		// Send:	T
		// Modtag:	T S # kg // # er den nye tara, punktum bruges som decimaltegn.
		// Kontroller brutto vægt.
		// Send:	D Brutto kontrol ok.
		// Modtag:	D A
		// Vent 2 sekunder.
		// Opdater lagerbeholdningen, skriv til loggen og retuner til step 1.
		
		writer.writeBytes("RM20 4 \"Ryd vægten.\" \" \" \"1/0\"\r\n");
		if (!reader.readLine().equals("RM20 B")) {
			step6error1();
			return;
		}
		String response = RM20(reader.readLine());
		if (response == null) {
			step6error1();
			return;
		} else if (response.equals("0")) {
			step5();
			return;
		} else if (!response.equals("1")) {
			step6error1();
			return;
		}
		
		//# TODO: Mangler noget kode her...
		
		log(new Date() + "," + operatorNumber + "," + operatorName + "," + productNumber + "," + productName + "," + tara + "," + netto);
		step1();
		
	}

	private void step6error1() throws Exception {
		
		// Step 6. Fejlet (1).
		// -------------------
		// Send: D Ugyldigt input.
		// Modtag: D A
		// Vent 2 sekunder.
		// Gentag step 6.
		
		writer.writeBytes("D Ugyldigt input.\r\n");
		if (!reader.readLine().equals("D A")) {
			step6error1();
			return;
		}
		Thread.sleep(2000);
		step6();
		
	}

	private void step6error2() throws Exception {
		
		// Step 6. Fejlet (2).
		// -------------------
		// Send: D Brutto kontrol fejlet.
		// Modtag: D A
		// Vent 2 sekunder.
		// Gentag step 6.
		

		writer.writeBytes("D Brutto kontrol fejlet.\r\n");
		if (!reader.readLine().equals("D A")) {
			step6error2();
			return;
		}
		Thread.sleep(2000);
		step6();

	}

	public static String getOperatorName(int number) {

		final Pattern pattern = Pattern.compile("^([0-9]+),([^,]+)$");

		Scanner scanner = null;
		
		try {
			scanner = new Scanner(new FileReader("operators.txt"));
			String line;
			while (scanner.hasNextLine()) {
				line = scanner.nextLine();
				Matcher matcher = pattern.matcher(line);
				if (!matcher.matches()) {
					System.err.println("Fejl i Operators.txt, linjen er ugyldig: " + line);
					return null;
				}
				if (Integer.parseInt(matcher.group(1)) == number) {
					return matcher.group(2);
				}
			}
			return null;
		} catch (FileNotFoundException e) {
			System.err.println("Filen blev ikke fundet: Operators.txt");
			return null;
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
		
	}
	
	public static String getProductName(int number) {

		final Pattern pattern = Pattern.compile("^([0-9]+),([^,]+)$");

		Scanner scanner = null;
		
		try {
			scanner = new Scanner(new FileReader("store.txt"));
			String line;
			while (scanner.hasNextLine()) {
				line = scanner.nextLine();
				Matcher matcher = pattern.matcher(line);
				if (!matcher.matches()) {
					System.err.println("Fejl i Store.txt, linjen er ugyldig: " + line);
					return null;
				}
				if (Integer.parseInt(matcher.group(1)) == number) {
					return matcher.group(2);
				}
			}
			return null;
		} catch (FileNotFoundException e) {
			System.err.println("Filen blev ikke fundet: Store.txt");
			return null;
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
		
	}
	
	private String RM20(String line) {

		final Pattern pattern = Pattern.compile("^RM20 A \"([^\"]*)\"$");

		Matcher matcher = pattern.matcher(line);
		if (!matcher.matches()) {
			return null;
		}
		return matcher.group(1);

	}

	private String T(String line) {

		final Pattern pattern = Pattern.compile("^T S +([0-9]*\\.?[0-9]+) kg$");

		Matcher matcher = pattern.matcher(line);
		if (!matcher.matches()) {
			return null;
		}
		return matcher.group(1);

	}
	
	private String S(String line) {

		final Pattern pattern = Pattern.compile("^S S +([0-9]*\\.?[0-9]+) kg$");

		Matcher matcher = pattern.matcher(line);
		if (!matcher.matches()) {
			return null;
		}
		return matcher.group(1);

	}
	
	public static void log(String message) {
		
		DataOutputStream writer = null;
		try {
			writer = new DataOutputStream(new FileOutputStream(new File("log.txt"), true));
			writer.writeBytes(message + "\r\n");
		} catch (Exception e) {
		} finally {
			try {
				writer.close();
			} catch (Exception e) {
			}
		}
		
	}

}