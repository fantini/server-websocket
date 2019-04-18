package br.gov.pr.server.module.print;

import java.io.IOException;
import java.util.Base64;
import java.util.logging.Logger;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;

public class Base64Print {
	
	private final static Logger LOGGER = Logger.getLogger(Base64Print.class.getName());

	public static void print(String source) throws PrintException, IOException {
		
		PrintService service = PrintServiceLookup.lookupDefaultPrintService();
		
		LOGGER.info("Initializing print service to "+service.getName());
		
		byte[] _source = Base64.getDecoder().decode(source);
		
		LOGGER.info("Source: "+source);
		
		PrintRequestAttributeSet  pras = new HashPrintRequestAttributeSet();
		
		pras.add(new Copies(1));		
		
		DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
		
		Doc doc = new SimpleDoc(_source, flavor, null);
		
		DocPrintJob job = service.createPrintJob();
		
		LOGGER.info("Send file to print service...");
		
		job.print(doc, pras);
	}
	
}
