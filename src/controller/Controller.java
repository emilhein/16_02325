package controller;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.ws.Response;

public class Controller {

	Socket socket = null;
	BufferedReader reader = null;
	DataOutputStream writer = null;
	
	int operatorNumber = 0;
	String operatorName = null;

	public Controller() {

		try {

			socket = new Socket("localhost", 4567);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new DataOutputStream(socket.getOutputStream());

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
		// Modtag: RM20 A "#" // # er den indtastede værdi.
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
		
		writer.writeBytes("RM20 4 \"Vare nummer:\" \" \" \" \"\r\n"); // Send: RM20 4 "Vare nummer:" "" ""
		reader.readLine().equals("RM20 B"); 	// Modtag: RM20 B
		String response = RM20(reader.readLine()); // Modtag: RM20 A "#" // # er den indtastede værdi.
		// Valider input og retuner til step 1 eller forsæt til step 3.
		
		if (response == null || !response.matches("^[0-9]+$")){
			step2error();
			return;
		} else if (response.equals("0")) {
			step1();
			return;
		}
		String exists = getProductName(Integer.parseInt(response));
		if(exists == null){
			step2error();
			return;
		}
		step3(exists);
		
	}		

	private void step2error() throws Exception {
		// // Step 2. Fejlet.
		// Send: D "Ukendt vare."
		// Modtag: D A
		// Vent 2 sekunder.
		// Send: DW // Er dette nødvendigt?
		// Modtag: DW A // Er dette nødvendigt?
		// Gentag step 2.
		//

		writer.writeBytes("D \"Ukendt vare.\" \r\n");
		if (!reader.readLine().equals("D A")) {	
			step2error();
			return;
		}
		Thread.sleep(2000);
		writer.writeBytes("DW\r\n");
		if (!reader.readLine().equals("DW A")) {	
			step2error();
			return;
		}
		step2();

	}

	private void step3(String productname) throws Exception {
		// // Step 3. Bekræft vare.
		// Send:	RM20 4 "Korrekt vare?" "#" "1/0" // # er vare navnet.
		// Modtag:	RM20 B
		// Modtag:	RM20 A "#" // # er den indtastede værdi.
		// Valider input og retuner til step 2 eller fortsæt til step 4.
		
		writer.writeBytes("RM20 4 \"Korrekt vare?\" \""  + productname + "\" \"1/0\"\r\n");
		if (!reader.readLine().equals("RM20 B")) {	
			step3error(productname);
			return;
		}
		String response = RM20(reader.readLine());
		if (response == null) {
			step3error(productname);
			return;
		}
		if (response.equals("0")) {
			step2();
			return;
		} else if (response.equals("1")) {
			step4();
			return;
		} else {
			step3error(productname);
			return;
		}
		
	}
	private void step3error(String Productname) throws Exception {
		// // Step 3. Fejlet.
		// Send:	D "Ugyldigt input."
		// Modtag:	D A
		// Vent 2 sekunder.
		// Send:	DW // Er dette nødvendigt?
		// Modtag:	DW A // Er dette nødvendigt?
		// Gentag step 3.
		// 	

		writer.writeBytes("D \"Ugyldigt input.\"\r\n");
		if (!reader.readLine().equals("D A")) {	
			step3error(Productname);
			return;
		}
		Thread.sleep(2000);
		writer.writeBytes("DW\r\n");
		if (!reader.readLine().equals("DW A")) {	
			step3error(Productname);
			return;
		}
		step3(Productname);
		
	}
	
	private void step4(String Productname) throws Exception {
		// // Step 4. Tarer vægt.
		// Send: RM20 4 "Placer skål på vægten." "" "1/0"
		// Modtag: RM20 B
		// Modtag: RM20 A "#" // # er den indtastede værdi.
		// Valider input og retuner til step 3 eller fortsæt.
		// Send: T
		// Modtag: T S # kg // # er den nye tara, punktum bruges som
		// decimaltegn.
		String response = "";
		step4loop: while(true){
			// Send: RM20 4 "Placer skål på vægten." "" "1/0"
			writer.writeBytes("RM20 4 \"Placer skål på vægten.\" \"\" \"1/0\"\r\n");

			// Modtag: RM20 B
			if (!reader.readLine().equals("RM20 B")) {
				step4error();
				return;
			}

			// Modtag: RM20 A "#" // # er den indtastede værdi.
			response = RM20(reader.readLine());
			if (response == null) {
				step4error();
				return;
			}

			// Valider input og retuner til step 3 eller fortsæt.
			if(response.equals("0") || response.equals("1"))
			{
				break step4loop;
			}
			step4error();

		}
		
		if(response.equals("0"))
			step3(Productname);
		
		// Send: S
		writer.writeBytes("T");

		// Modtag: T T # kg // # er den nye tara vægten, punktum bruges som decimaltegn.
		String Tara = T(reader.readLine());
		
	}

	private void step4error() throws Exception {
		// // Step 4. Fejlet.
		// Send: D "Ugyldigt input."
		// Modtag: D A
		// Vent 2 sekunder.
		// Send: DW // Er dette nødvendigt?
		// Modtag: DW A // Er dette nødvendigt?
		// Gentag step 4.
		//
		writer.writeBytes("D \"ugyldigt input.\"\r\n");

		if (!reader.readLine().equals("D A")) {
			step1error();
			return;
		}

	}

	private String T(String line) {

		final Pattern pattern = Pattern.compile("^S S ([0-9]*\\.?[0-9]+) kg$");

		Matcher matcher = pattern.matcher(line);
		if (!matcher.matches()) {
			return null;
		}
		return matcher.group(1);

	}

	private void step5() throws Exception {
		// // Step 5. Afvej vare.
		String response = "";
		step5loop: while(true){
			// Send: RM20 4 "Placer vare i skålen." "" "1/0"
			writer.writeBytes("RM20 4 \"Placer vare i skålen.\" \"\" \"1/0\"\r\n");

			// Modtag: RM20 B
			if (!reader.readLine().equals("RM20 B")) {
				step5error();
				return;
			}

			// Modtag: RM20 A "#" // # er den indtastede værdi.
			response = RM20(reader.readLine());
			if (response == null) {
				step5error();
				return;
			}

			// Valider input og retuner til step 4 eller fortsæt.
			if(response.equals("0") || response.equals("1"))
			{
				break step5loop;
			}
			step5error();

		}
		
		if(response.equals("0"))
			step4();
		
		// Send: S
		writer.writeBytes("S\r\n");

		// Modtag: S S # kg // # er netto vægten, punktum bruges som decimaltegn.
		String Netto = S(reader.readLine());
		
	}

	private void step5error() throws Exception{
		// // Step 5. Fejlet.
		// Send: D "Ugyldigt input."
		writer.writeBytes("D \"ugyldigt input.\"\r\n");

		// Modtag: D A
		if (!reader.readLine().equals("D A")) {
			step1error();
			return;
		}

	}

	private String S(String line) {

		final Pattern pattern = Pattern.compile("^S S ([0-9]*\\.?[0-9]+) kg$");

		Matcher matcher = pattern.matcher(line);
		if (!matcher.matches()) {
			return null;
		}
		return matcher.group(1);

	}

	
	private void step6() throws Exception{
		// /	/ Step 6. Kontroller brutto vægt.
					// Send:	RM20 4 "Ryd vægten." "" "1/0"
					// Modtag:	RM20 B
					// Modtag:	RM20 A "#" // # er den indtastede værdi.
					// Valider input og retuner til step 5 eller fortsæt.
					// Send:	T
					// Modtag:	T S # kg // # er den nye tara, punktum bruges som decimaltegn.
					// Kontroller brutto vægt.
					// Send:	D "Brutto kontrol ok."
					// Modtag:	D A
					// Vent 2 sekunder.
					// Send:	DW // Er dette nødvendigt?
					// Modtag:	DW A // Er dette nødvendigt?
					// Opdater lagerbeholdningen, skriv til loggen og retuner til step 1.
		writer.writeBytes("RM20 4 \"Ryd vægten:\" \"\" \"\"\r\n");
		if (reader.readLine().equals("RM20 B")) {
			String response = RM20(reader.readLine());
			if (response == null) {
				step6error();
			}
		} else
			step6error();
		return;
}

	private void step6error() {
		// // Step 6. Fejlet (1).
		// Send: D "Ugyldigt input."
		// Modtag: D A
		// Vent 2 sekunder.
		// Send: DW // Er dette nødvendigt?
		// Modtag: DW A // Er dette nødvendigt?
		// Gentag step 6.
		// // Step 6. Fejlet (2).
		// Send: D "Brutto kontrol fejlet."
		// Modtag: D A
		// Vent 2 sekunder.
		// Send: DW // Er dette nødvendigt?
		// Modtag: DW A // Er dette nødvendigt?
		// Gentag step 6.

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

		final Pattern pattern = Pattern.compile("^RM20 A ([^\"]*)$");

		Matcher matcher = pattern.matcher(line);
		if (!matcher.matches()) {
			return null;
		}
		return matcher.group(1);

	}

}
