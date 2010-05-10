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
import android.util.Log;

import com.liato.urllib.Urllib;

public class BankICA implements Bank {

	private String username;
	private String password;
	private Banks banktype = Banks.ICA;
	private Pattern reEventValidation = Pattern.compile("__EVENTVALIDATION\"\\s+value=\"([^\"]+)\"");
	private Pattern reViewState = Pattern.compile("__VIEWSTATE\"\\s+value=\"([^\"]+)\"");
	private Pattern reError = Pattern.compile("<label\\s+class=\"error\">(.+?)</label>",Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private Pattern reBalance = Pattern.compile("account\\.aspx\\?id=([^\"]+).+?>([^<]+)</a.+?Saldo([0-9 .,-]+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	
	private ArrayList<Account> accounts = new ArrayList<Account>();
	private BigDecimal balance = new BigDecimal(0);

	public BankICA() {
	}

	public BankICA(String username, String password) throws BankException {
		this.update(username, password);
	}

	public void update(String username, String password) throws BankException {
		this.username = username;
		this.password = password;
		this.update();
	}
	public void update() throws BankException {
		if (username == null || password == null || username.length() == 0 || password.length() == 0) {
			throw new BankException("Personnummer och lösenord stämmer ej.");//res.getText(R.string.invalid_username_password));
		}

		Urllib urlopen = new Urllib();
		String response = null;
		Matcher matcher;
		try {
			response = urlopen.open("https://mobil.icabanken.se/login/login.aspx");
			matcher = reViewState.matcher(response);
			if (!matcher.find()) {
				throw new BankException("Could not connect to the bank. Unable to parse data.");
			}
			String strViewState = matcher.group(1);
			matcher = reEventValidation.matcher(response);
			if (!matcher.find()) {
				throw new BankException("Could not connect to the bank. Unable to parse data.");
			}
			String strEventValidation = matcher.group(1);
			List <NameValuePair> postData = new ArrayList <NameValuePair>();
			postData.add(new BasicNameValuePair("pnr_phone", username));
			postData.add(new BasicNameValuePair("pwd_phone", password));
			postData.add(new BasicNameValuePair("btnLogin", "Logga in"));
			postData.add(new BasicNameValuePair("__VIEWSTATE", strViewState));
			postData.add(new BasicNameValuePair("__EVENTVALIDATION", strEventValidation));
			response = urlopen.open("https://mobil.icabanken.se/login/login.aspx", postData);
			
			matcher = reError.matcher(response);
			if (matcher.find()) {
				throw new BankException(Html.fromHtml(matcher.group(1).trim()).toString());
			}

			response = urlopen.open("https://mobil.icabanken.se/account/overview.aspx");
			matcher = reBalance.matcher(response);
			while (matcher.find()) {
				Log.d("BankICA", "Saldo: "+matcher.group(3));
				accounts.add(new Account(Html.fromHtml(matcher.group(2)).toString().trim(), Helpers.parseBalance(matcher.group(3).trim()), matcher.group(1).trim()));
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