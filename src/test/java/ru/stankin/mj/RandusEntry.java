package ru.stankin.mj;

/**
 * Created by nickl-mac on 13.09.15.
 */

// {"lname":"Осипова","fname":"Элизабет","patronymic":"Ильинична ","date":"28 февраля 1972",
// "city":"Новый Уренгой","street":"Советская","house":93,"apartment":238,"phone":"8-919-804-66-83",
// "login":"OsipovaElizabet46","password":"tSa51tZQYDks","color":"Голубой","userpic":"http://randus.ru/avatars/w/myAvatar21.png"}


public class RandusEntry {

    public String lname;
    public String fname;
    public String patronymic;
    public String date;
    public String city;
    public String street;
    public String phone;
    public int house;
    public int apartment;
    public String login;
    public String password;
    public String color;
    public String userpic;

    @Override
    public String toString() {
        return "RandusEntry{" +
                "apartment=" + apartment +
                ", lname='" + lname + '\'' +
                ", fname='" + fname + '\'' +
                ", patronymic='" + patronymic + '\'' +
                ", date='" + date + '\'' +
                ", city='" + city + '\'' +
                ", street='" + street + '\'' +
                ", phone='" + phone + '\'' +
                ", house=" + house +
                ", login='" + login + '\'' +
                ", password='" + password + '\'' +
                ", color='" + color + '\'' +
                ", userpic='" + userpic + '\'' +
                '}';
    }

    public String gender(){
        char c = userpic.charAt(25);
        //System.err.println("c="+c + " "+userpic);
        switch (c){
            case 'w': return "ж";
            case 'm': return "м";
        }
        throw new IllegalArgumentException("unknown char "+ c);
    }

}
