package tech.myic.mdnutils;

public class HeaderValue
{
    private final String name;
    private final String value;

    public HeaderValue(String name, String value)
    {
        this.name = name;
        this.value = value;
    }

    public String getName()
    {
        return name;
    }

    public String getValue()
    {
        return value;
    }
}
