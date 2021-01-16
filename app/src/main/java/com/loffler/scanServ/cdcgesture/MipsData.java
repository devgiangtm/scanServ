package com.loffler.scanServ.cdcgesture;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.Calendar;

public class MipsData implements Parcelable {
    public static final Creator<MipsData> CREATOR = new Creator<MipsData>() {
        public MipsData createFromParcel(Parcel param1Parcel) {
            return new MipsData(param1Parcel);
        }

        public MipsData[] newArray(int param1Int) {
            return new MipsData[param1Int];
        }
    };

    @SerializedName("address")
    private String address;

    @SerializedName("birthdate")
    private String birthdate;

    @SerializedName("cardNo")
    private String cardNo;

    @SerializedName("cardPhoto")
    private String cardPhoto;

    @SerializedName("checkPic")
    private String checkPic;

    @SerializedName("checkTime")
    private long checkTime;

    @SerializedName("extra")
    private String extra;

    @SerializedName("idCardNo")
    private String idCardNo;

    @SerializedName("mac")
    private String mac;

    @SerializedName("mask")
    private int mask;

    @SerializedName("name")
    private String name;

    @SerializedName("nation")
    private String nation;

    @SerializedName("sex")
    private String sex;

    @SerializedName("temperature")
    private String temperature;

    @SerializedName("type")
    private int type;

    @SerializedName("userId")
    private String userId;

    public MipsData() {}

    protected MipsData(Parcel paramParcel) {
        this.birthdate = paramParcel.readString();
        this.address = paramParcel.readString();
        this.nation = paramParcel.readString();
        this.idCardNo = paramParcel.readString();
        this.sex = paramParcel.readString();
        this.type = paramParcel.readInt();
        this.userId = paramParcel.readString();
        this.cardNo = paramParcel.readString();
        this.mac = paramParcel.readString();
        this.cardPhoto = paramParcel.readString();
        this.checkTime = paramParcel.readLong();
        this.extra = paramParcel.readString();
        this.name = paramParcel.readString();
        this.temperature = paramParcel.readString();
        this.checkPic = paramParcel.readString();
        this.mask = paramParcel.readInt();
    }

    public static MipsData getTestData() {
       MipsData mipsData = new MipsData();
        mipsData.setUserId("{userId}");
        mipsData.setAddress("{userAddress}");
        mipsData.setCheckTime(Calendar.getInstance().getTimeInMillis());
        mipsData.setType(1);
        mipsData.setTemperature("38");
        mipsData.setMask(0);
        mipsData.setName("{userName}");
        return mipsData;
    }

    public int describeContents() {
        return 0;
    }

    public String getAddress() {
        return this.address;
    }

    public String getBirthdate() {
        return this.birthdate;
    }

    public String getCardNo() {
        return this.cardNo;
    }

    public String getCardPhoto() {
        return this.cardPhoto;
    }

    public String getCheckPic() {
        return this.checkPic;
    }

    public long getCheckTime() {
        return this.checkTime;
    }

    public String getExtra() {
        return this.extra;
    }

    public String getIdCardNo() {
        return this.idCardNo;
    }

    public String getMac() {
        return this.mac;
    }

    public int getMask() {
        return this.mask;
    }

    public String getName() {
        return this.name;
    }

    public String getNation() {
        return this.nation;
    }

    public String getSex() {
        return this.sex;
    }

    public String getTemperature() {
        return this.temperature;
    }

    public int getType() {
        return this.type;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setAddress(String paramString) {
        this.address = paramString;
    }

    public void setBirthdate(String paramString) {
        this.birthdate = paramString;
    }

    public void setCardNo(String paramString) {
        this.cardNo = paramString;
    }

    public void setCardPhoto(String paramString) {
        this.cardPhoto = paramString;
    }

    public void setCheckPic(String paramString) {
        this.checkPic = paramString;
    }

    public void setCheckTime(long paramLong) {
        this.checkTime = paramLong;
    }

    public void setExtra(String paramString) {
        this.extra = paramString;
    }

    public void setIdCardNo(String paramString) {
        this.idCardNo = paramString;
    }

    public void setMac(String paramString) {
        this.mac = paramString;
    }

    public void setMask(int paramInt) {
        this.mask = paramInt;
    }

    public void setName(String paramString) {
        this.name = paramString;
    }

    public void setNation(String paramString) {
        this.nation = paramString;
    }

    public void setSex(String paramString) {
        this.sex = paramString;
    }

    public void setTemperature(String paramString) {
        this.temperature = paramString;
    }

    public void setType(int paramInt) {
        this.type = paramInt;
    }

    public void setUserId(String paramString) {
        this.userId = paramString;
    }

    public void writeToParcel(Parcel paramParcel, int paramInt) {
        paramParcel.writeString(this.birthdate);
        paramParcel.writeString(this.address);
        paramParcel.writeString(this.nation);
        paramParcel.writeString(this.idCardNo);
        paramParcel.writeString(this.sex);
        paramParcel.writeInt(this.type);
        paramParcel.writeString(this.userId);
        paramParcel.writeString(this.cardNo);
        paramParcel.writeString(this.mac);
        paramParcel.writeString(this.cardPhoto);
        paramParcel.writeLong(this.checkTime);
        paramParcel.writeString(this.extra);
        paramParcel.writeString(this.name);
        paramParcel.writeString(this.temperature);
        paramParcel.writeString(this.checkPic);
        paramParcel.writeInt(this.mask);
    }
}
