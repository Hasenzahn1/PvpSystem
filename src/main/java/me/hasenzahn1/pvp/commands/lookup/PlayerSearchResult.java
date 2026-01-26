package me.hasenzahn1.pvp.commands.lookup;

import java.util.List;

public class PlayerSearchResult {

    private static final int ENTRIES_PER_PAGE = 10;

    private int page;
    private final List<LookupEntry> entries;

    public PlayerSearchResult(List<LookupEntry> entries) {
        this.page = 0;
        this.entries = entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void getCurrentPage(int page) {
        this.page = page;
    }

    public List<LookupEntry> getEntriesForPage() {
        this.page = Math.max(0, Math.min(this.page, Math.floorDiv(entries.size(), ENTRIES_PER_PAGE)));
        System.out.println("currentPage: " + this.page);
        System.out.println("StartIndex: " + this.page * ENTRIES_PER_PAGE);
        System.out.println("EndIndex: " + Math.min((this.page + 1) * ENTRIES_PER_PAGE,  entries.size()));
        return entries.subList(this.page * ENTRIES_PER_PAGE, Math.min((this.page + 1) * ENTRIES_PER_PAGE,  entries.size()));
    }

    public int getPage(){
        return this.page;
    }

    public void setPage(int page){
        this.page = Math.max(0, Math.min(page, Math.floorDiv(entries.size(), ENTRIES_PER_PAGE)));;
    }

    public int getMaxPages(){
        return Math.ceilDiv(entries.size(), ENTRIES_PER_PAGE);
    }

    public int getEntryCount(){
        return entries.size();
    }

}
