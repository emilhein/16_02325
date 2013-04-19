package controller;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.LineNumberReader;

public class Controller {

	Socket socket = null;
	BufferedReader reader = null;
	DataOutputStream writer = null;

	public Controller() {

		try {

			socket = new Socket("localhost", 4567);
			reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			writer = new DataOutputStream(socket.getOutputStream());

			step1();
			step2();
			step3();
			step4();
			step5();
			step6();

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

		writer.writeBytes("RM20 4 \"Operatør nummer:\" \"\" \"\"");
		if (!reader.readLine().equals("RM20 B")) {
			step1error();
			return;
		}
		String response = RM20(reader.readLine());
		if (response == null) {
			step1error();
			return;
		}
		step2();

	}

	private void step1error() {

		// Step 1. Fejlet.
		// ---------------
		// Send: D "Ukendt operatør."
		// Modtag: D A
		// Vent 2 sekunder.
		// Send: DW // Er dette nødvendigt?
		// Modtag: DW A // Er dette nødvendigt?
		// Gentag step 1.

	}

	private void step2() throws Exception {

		// Step 2. Identificer vare.
		// -------------------------
		// Send: RM20 4 "Vare nummer:" "" ""
		// Modtag: RM20 B
		// Modtag: RM20 A "#" // # er den indtastede værdi.
		// Valider input og retuner til step 1 eller forsæt til step 3.
		writer.writeBytes("RM20 4 \"Vare nummer:\" \"\" \"\"");
		reader.readLine().equals("RM20 B"); 
		String response = RM20(reader.readLine());
		if (response == null){
			step2error();
		}
		int item = Integer.parseInt(response);
		readstore(item);
			
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
		System.out.println("Ugyldigt input i step2");
		writer.writeBytes("D \"ugyldigt input.\"");
		if (!reader.readLine().equals("D A")) {	
			step2error();
			return;
		}
		
		System.out.println("vent to sekunder!!");
		writer.writeBytes("DW");
		if (!reader.readLine().equals("DW A")) {	
			step2error();
			return;
		}
		
		step2();
		}



	private void step3() throws Exception {
		// // Step 3. Bekræft vare.
		// Send:	RM20 4 "Korrekt vare?" "#" "1/0" // # er vare navnet.
		// Modtag:	RM20 B
		// Modtag:	RM20 A "#" // # er den indtastede værdi.
		// Valider input og retuner til step 2 eller fortsæt til step 4.
		
		// hvor skal jeg vide hvad vare navnet er ??
		writer.writeBytes("RM20 4 \"Korrekt vare?\" \"#\" \"1/0\"");
		if (!reader.readLine().equals("RM20 B")) {	
			step3error();
			return;
		}
		String response = RM20(reader.readLine());
		if (response == null) {
			step3error();
			return;
		}
		if (!response.equals("1")) {
			step2();
			return;
		}
		step4();
		
	}
	private void step3error() throws Exception {
		// // Step 3. Fejlet.
		// Send:	D "Ugyldigt input."
		// Modtag:	D A
		// Vent 2 sekunder.
		// Send:	DW // Er dette nødvendigt?
		// Modtag:	DW A // Er dette nødvendigt?
		// Gentag step 3.
		// 	
		System.out.println("Ugyldigt input i step3");
		writer.writeBytes("D \"ugyldigt input.\"");
		if (!reader.readLine().equals("D A")) {	
			step3error();
			return;
		}
		
		System.out.println("vent to sekunder!!");
		writer.writeBytes("DW");
		if (!reader.readLine().equals("DW A")) {	
			step3error();
			return;
		}
		
		step3();
	}
	


	private void step4() throws Exception {
		// // Step 4. Tarer vægt.
		// Send: RM20 4 "Placer skål på vægten." "" "1/0"
		// Modtag: RM20 B
		// Modtag: RM20 A "#" // # er den indtastede værdi.
		// Valider input og retuner til step 3 eller fortsæt.
		// Send: T
		// Modtag: T S # kg // # er den nye tara, punktum bruges som
		// decimaltegn.
	}

	private void step4error() {
		// // Step 4. Fejlet.
		// Send: D "Ugyldigt input."
		// Modtag: D A
		// Vent 2 sekunder.
		// Send: DW // Er dette nødvendigt?
		// Modtag: DW A // Er dette nødvendigt?
		// Gentag step 4.
		//

	}

	private void step5() throws Exception {
		// // Step 5. Afvej vare.
		// Send: RM20 4 "Placer vare i skålen." "" "1/0"
		// Modtag: RM20 B
		// Modtag: RM20 A "#" // # er den indtastede værdi.
		// Valider input og retuner til step 4 eller fortsæt.
		// Send: S
		// Modtag: S S # kg // # er netto vægten, punktum bruges som
		// decimaltegn.
	}

	private void step5error() {
		// // Step 5. Fejlet.
		// Send: D "Ugyldigt input."
		// Modtag: D A
		// Vent 2 sekunder.
		// Send: DW // Er dette nødvendigt?
		// Modtag: DW A // Er dette nødvendigt?
		// Gentag step 5.
		//

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
		writer.writeBytes("RM20 4 \"Ryd vægten:\" \"\" \"\"");
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

	public static void readstore(int a) {
		LineNumberReader br = null;
		try {

			br = new LineNumberReader(new FileReader("store.txt"));
			for (String line = null; (line = br.readLine()) != null;) {
				if (br.getLineNumber() == a) {
					System.out.println(br.readLine());
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
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

}
