package se.chs.gud;

public class WhitelistEntry {
    private String uuid;
    private String name;

    public WhitelistEntry(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public String getUniqueId() {
        return uuid;
    }

    public void setUniqueId(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "WhitelistEntry{" +
                "uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}