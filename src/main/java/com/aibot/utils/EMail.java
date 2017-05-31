package com.aibot.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EMail {
	
	public String subject;
	public String fromAddress;
	public String toAddresses;
	public String cc;
	public String bcc;
	public String body;
	
	public boolean doNotRedirect;
	
	private List<String[]> embedList=new ArrayList<String[]>();
	
	/**
	 * Empty constructor
	 */
	public EMail () {
		
	}
	
	/**
	 * Constructor
	 * 
	 * @param from
	 * @param to
	 * @param subject
	 * @param content
	 */
	public EMail(String from, String to, String subject, String content) {
		this.subject =subject;
		this.fromAddress = from;
		this.toAddresses = to;
		this.body =content;
	}
			
	/**
	 * 
	 * @return
	 */
	public boolean hasEmbed() {
		return !this.embedList.isEmpty();
	}

	/**
	 * Add on embed link
	 * 
	 * @param cid
	 * @param uri
	 */
	public void addEmbed(String cid, String uri) {
		if (hasEmbed(uri)) {
			return;
		}
		String[] embed=new String[] {cid, uri};
		this.embedList.add(embed); 
	}
	
	private boolean hasEmbed(String uri) {
		String[] one;
		for (Iterator<String[]> iter=this.embedList.iterator(); iter.hasNext();) {
			one =iter.next();
			if (one[1].toString().equalsIgnoreCase(uri)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Add all embeds
	 * @param embeds
	 */
	public void selectEmbed(List<String[]> embeds) {
		String[] oneEmbed;
		for (Iterator<String[]> iter=embeds.iterator(); iter.hasNext();) {
			oneEmbed=iter.next();
			if (oneEmbed !=null && oneEmbed.length==2 && !StringUtils.isEmpty(oneEmbed[0])) {
				if (this.body.indexOf("cid:"+oneEmbed[0]) >=0
						&& !hasEmbed(oneEmbed[1])) {
					this.embedList.add(oneEmbed );
				}
			}
		}
		
		//this.embedList.addAll(embeds);
		
	}
	public List<String[]> getEmbedList() {
		return this.embedList;
	}
	
	/**
	 * 
	 * @return
	 */
	public String concatEmbed() {
		return concatEmbed(this.embedList);
	}
	
	/**
	 * 
	 * @return
	 */
	public static String concatEmbed(List<String[]> attached) {
		StringBuffer sb=new StringBuffer();
		String[] oneEmbed;
		for (Iterator<String[]> iter= attached.iterator(); iter.hasNext(); ) {
			oneEmbed = iter.next();
			if (sb.length()!=0) {
				sb.append("|");
			}
			sb.append(EncryptUtil.base64(oneEmbed[0])).append(",");
			sb.append(EncryptUtil.base64(oneEmbed[1]));
		}
		return sb.toString();
	}
	
	/**
	 * Reconstruct the embed from the base64 encrypted string 
	 * @param input
	 */
	public void constructEmbedList(String input) {
		
		String[] embeds= input.split("\\|");
		if (embeds ==null) {
			return;
		}
		
		for (int i=0; i<embeds.length;i++) {
			if (embeds[i] !=null) {
				String[] infos=embeds[i].split(",")	;
				if (infos !=null && infos.length==2) {
					this.addEmbed(EncryptUtil.base64Decode(infos[0]), EncryptUtil.base64Decode(infos[1]));
				}
			}
			
		}
	}
	
	/**
	 * Print all embed links 
	 */
	public void printEmbed() {
		String[] oneEmbed;
		for (Iterator<String[]> iter= this.embedList.iterator(); iter.hasNext(); ) {
			oneEmbed = iter.next();
			System.out.print(oneEmbed[0]+" : ");
			System.out.println(oneEmbed[1]);
		}	
	}
	/**
	 * TEst
	 * @param argv
	 */
//	public static void main(String[] argv) {
//		EMail mail =new EMail();
//		mail.addEmbed("one", "file://dsdsds,,dsdsd");
//		mail.addEmbed("two", "file://dsdsds|||dsdsd");
//
//		String x=mail.concatEmbed();
//		System.out.println(x);
//		mail.constructEmbedList(x);
//		mail.printEmbed();
//	}

	public boolean isDoNotRedirect() {
		return doNotRedirect;
	}

	public void setDoNotRedirect(boolean doNotRedirect) {
		this.doNotRedirect = doNotRedirect;
	}
}


