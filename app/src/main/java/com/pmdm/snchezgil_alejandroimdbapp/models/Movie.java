package com.pmdm.snchezgil_alejandroimdbapp.models;

import android.os.Parcel;
import android.os.Parcelable;
//Clase movie de solo datos y parcelable.
public class Movie implements Parcelable {
    private String imageUrl;
    private String title;
    private String rank;
    private String rating;
    private String id;
    private String descripcion;
    private String fecha;
    private boolean cargada;

    public Movie() {
        this.cargada = false;
    }

    protected Movie(Parcel in) {
        imageUrl = in.readString();
        title = in.readString();
        rank = in.readString();
        rating = in.readString();
        id = in.readString();
        descripcion = in.readString();
        fecha = in.readString();
    }

    public static final Creator<Movie> CREATOR = new Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel in) {
            return new Movie(in);
        }

        @Override
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };

    public String getImageUrl() {return imageUrl;}
    public void setImageUrl(String imageUrl) {this.imageUrl = imageUrl;}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRank() {return rank;}
    public void setRank(String rank) {this.rank = rank;}

    public String getRating() {return rating;}
    public void setRating(String rating) {this.rating = rating;}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public boolean isCargada() {
        return cargada;
    }

    public void setCargada(boolean cargada) {
        this.cargada = cargada;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(imageUrl);
        parcel.writeString(title);
        parcel.writeString(rank);
        parcel.writeString(rating);
        parcel.writeString(id);
        parcel.writeString(descripcion);
        parcel.writeString(fecha);
    }
}
