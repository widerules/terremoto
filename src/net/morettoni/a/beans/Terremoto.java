package net.morettoni.a.beans;

import java.util.Date;

import com.google.android.maps.GeoPoint;

public class Terremoto {
	private long id;
	private double magnitude;
	private String luogo;
	private Date data;
	private double profondita;
	private double latitudine;
	private double longitudine;
	
	public Terremoto() {
	}
	
	public GeoPoint getGeoPoint() {
		Double lat = latitudine * 1E6;
		Double lng = longitudine * 1E6;
		return new GeoPoint(lat.intValue(), lng.intValue());
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

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
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
