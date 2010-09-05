package net.morettoni.a.beans;

import java.util.Date;

public class Terremoto {
	private double magnitude;
	private String luogo;
	private Date data;
	private String link;
	private double profondita;
	private double latitudine;
	private double longitudine;
	
	public Terremoto() {
	}

	public Terremoto(double magnitude, double profondita, String luogo, Date data) {
		this.magnitude = magnitude;
		this.profondita = profondita;
		this.luogo = luogo;
		this.data = data;
	}

	public Terremoto(double magnitude, double profondita, String luogo, Date data, String link) {
		this.magnitude = magnitude;
		this.profondita = profondita;
		this.luogo = luogo;
		this.data = data;
		this.link = link;
	}
	
	public Terremoto(double magnitude, double profondita, String luogo, Date data, String link, double latitudine, double longitudine) {
		this.magnitude = magnitude;
		this.profondita = profondita;
		this.luogo = luogo;
		this.data = data;
		this.link = link;
		this.latitudine = latitudine;
		this.longitudine = longitudine;
	}
	
	@Override
	public String toString() {
		return String.format("(%.1f) %s", magnitude, luogo);
	}

	public Date getData() {
		return data;
	}

	public void setData(Date data) {
		this.data = data;
	}

	public double getMagnitude() {
		return magnitude;
	}

	public void setMagnitude(double magnitude) {
		this.magnitude = magnitude;
	}

	public double getProfondita() {
		return profondita;
	}

	public void setProfondita(double profondita) {
		this.profondita = profondita;
	}

	public String getLuogo() {
		return luogo;
	}

	public void setLuogo(String luogo) {
		this.luogo = luogo;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public double getLatitudine() {
		return latitudine;
	}

	public void setLatitudine(double latitudine) {
		this.latitudine = latitudine;
	}

	public double getLongitudine() {
		return longitudine;
	}

	public void setLongitudine(double longitudine) {
		this.longitudine = longitudine;
	}
}
