package com.bike.webapp.dto;

public class HazardStats {
    private int total;
    private int pending;
    private int reviewing;
    private int approved;
    private int rejected;

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getPending() { return pending; }
    public void setPending(int pending) { this.pending = pending; }
    public int getReviewing() { return reviewing; }
    public void setReviewing(int reviewing) { this.reviewing = reviewing; }
    public int getApproved() { return approved; }
    public void setApproved(int approved) { this.approved = approved; }
    public int getRejected() { return rejected; }
    public void setRejected(int rejected) { this.rejected = rejected; }
}
