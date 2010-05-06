package com.liato.bankdroid;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import android.text.Html;
import com.liato.urllib.Urllib;

public class BankNordea implements Bank {

	private String username;
	private String password;
	private Banks banktype = Banks.NORDEA;
	private Pattern reBalance = Pattern.compile("(?is)nowrap>(.+?)SEK<", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private Pattern reAccounts = Pattern.compile("(?is)Kontoutdraget';.*?>(.*?)</a></td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private ArrayList<Account> accounts = new ArrayList<Account>();
	private BigDecimal balance = new BigDecimal(0);

	public BankNordea() {
	}

	public BankNordea(String username, String password) throws BankException {
		this.update(username, password);
	}

	public void update(String username, String password) throws BankException {
		this.username = username;
		this.password = password;
		this.update();
	}
	public void update() throws BankException {
		if (username == null || password == null || username.length() == 0 || password.length() == 0) {
			throw new BankException("Personnummer och l�senord st�mmer ej.");
		}
		Urllib urlopen = new Urllib();
		String response = null;
		Matcher matcherBalance;
		Matcher matcherAccounts;
		try {
			response = urlopen.open("https://gfs.nb.se/bin2/gfskod?OBJECT=KK20");
			List <NameValuePair> postData = new ArrayList <NameValuePair>();
			postData.add(new BasicNameValuePair("kundnr", username));
			postData.add(new BasicNameValuePair("pinkod", password));
			postData.add(new BasicNameValuePair("OBJECT", "TT00"));
			postData.add(new BasicNameValuePair("prev_link", "https://gfs.nb.se/privat/bank/login_kod2.html"));
			postData.add(new BasicNameValuePair("CHECKCODE", "checkcode"));
			response = urlopen.open("https://gfs.nb.se/bin2/gfskod", postData);

			if (!response.contains("reDirect")) {
				throw new BankException("Personnummer och l�senord st�mmer ej.");
			}

			response = urlopen.open("https://gfs.nb.se/bin2/gfskod?OBJECT=KF00T&show_button=No");
			matcherBalance = reBalance.matcher(response);
			matcherAccounts = reAccounts.matcher(response);
			while (matcherAccounts.find() && matcherBalance.find()) {
				accounts.add(new Account(Html.fromHtml(matcherAccounts.group(1)).toString(), Helpers.parseBalance(matcherBalance.group(1))));
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			urlopen.close();
		}

	}


	@Override
	public ArrayList<Account> getAccounts() {
		return this.accounts;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public Banks getType() {
		return banktype;
	}

	@Override
	public String getUsername() {
		return username;
	}
	
	@Override
	public BigDecimal getBalance() {
		return balance;
	}	
}