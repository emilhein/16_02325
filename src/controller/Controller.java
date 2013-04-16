package controller;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class Controller {
	
	Socket socket = null;
	BufferedReader reader = null;
	DataOutputStream writer = null;
	
	public Controller() {
		
		try {
			
			socket = new Socket("localhost", 8080);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new DataOutputStream(socket.getOutputStream());
			
			step1();
			
			// // Step 2. Fejlet.
			// Send:	D "Ukendt vare."
			// Modtag:	D A
			// Vent 2 sekunder.
			// Send:	DW // Er dette nødvendigt?
			// Modtag:	DW A // Er dette nødvendigt?
			// Gentag step 2.
			// 
			// // Step 3. Bekræft vare.
			// Send:	RM20 4 "Korrekt vare?" "#" "1/0" // # er vare navnet.
			// Modtag:	RM20 B
			// Modtag:	RM20 A "#" // # er den indtastede værdi.
			// Valider input og retuner til step 2 eller fortsæt til step 4.
			// // Step 3. Fejlet.
			// Send:	D "Ugyldigt input."
			// Modtag:	D A
			// Vent 2 sekunder.
			// Send:	DW // Er dette nødvendigt?
			// Modtag:	DW A // Er dette nødvendigt?
			// Gentag step 3.
			// 
			// // Step 4. Tarer vægt.
			// Send:	RM20 4 "Placer skål på vægten." "" "1/0"
			// Modtag:	RM20 B
			// Modtag:	RM20 A "#" // # er den indtastede værdi.
			// Valider input og retuner til step 3 eller fortsæt.
			// Send:	T
			// Modtag:	T S # kg // # er den nye tara, punktum bruges som decimaltegn.
			// // Step 4. Fejlet.
			// Send:	D "Ugyldigt input."
			// Modtag:	D A
			// Vent 2 sekunder.
			// Send:	DW // Er dette nødvendigt?
			// Modtag:	DW A // Er dette nødvendigt?
			// Gentag step 4.
			// 
			// // Step 5. Afvej vare.
			// Send:	RM20 4 "Placer vare i skålen." "" "1/0"
			// Modtag:	RM20 B
			// Modtag:	RM20 A "#" // # er den indtastede værdi.
			// Valider input og retuner til step 4 eller fortsæt.
			// Send:	S
			// Modtag:	S S # kg // # er netto vægten, punktum bruges som decimaltegn.
			// // Step 5. Fejlet.
			// Send:	D "Ugyldigt input."
			// Modtag:	D A
			// Vent 2 sekunder.
			// Send:	DW // Er dette nødvendigt?
			// Modtag:	DW A // Er dette nødvendigt?
			// Gentag step 5.
			// 
			// // Step 6. Kontroller brutto vægt.
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
			// // Step 6. Fejlet (1).
			// Send:	D "Ugyldigt input."
			// Modtag:	D A
			// Vent 2 sekunder.
			// Send:	DW // Er dette nødvendigt?
			// Modtag:	DW A // Er dette nødvendigt?
			// Gentag step 6.
			// // Step 6. Fejlet (2).
			// Send:	D "Brutto kontrol fejlet."
			// Modtag:	D A
			// Vent 2 sekunder.
			// Send:	DW // Er dette nødvendigt?
			// Modtag:	DW A // Er dette nødvendigt?
			// Gentag step 6.
	
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
		// Send:	RM20 4 "Operatør nummer:" "" ""
		// Modtag:	RM20 B
		// Modtag:	RM20 A "#" // # er den indtastede værdi.
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
		// Send:	D "Ukendt operatør."
		// Modtag:	D A
		// Vent 2 sekunder.
		// Send:	DW // Er dette nødvendigt?
		// Modtag:	DW A // Er dette nødvendigt?
		// Gentag step 1.
		
	}
	private void step2() {
	
		// Step 2. Identificer vare.
		// -------------------------
		// Send:	RM20 4 "Vare nummer:" "" ""
		// Modtag:	RM20 B
		// Modtag:	RM20 A "#" // # er den indtastede værdi.
		// Valider input og retuner til step 1 eller forsæt til step 3.
		
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