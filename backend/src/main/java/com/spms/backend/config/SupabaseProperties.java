//YENI EKLENEN JAVA CLASSI. SUPABASE BAGLANTISI İÇİN GEREKLİDİR.

package com.spms.backend.config;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supabase")

public class SupabaseProperties {
    private String url;
    private String serviceKey;
    public String getUrl()        { return url; }
    public void setUrl(String u)  { this.url = u; }
    public String getServiceKey()        { return serviceKey; }
    public void setServiceKey(String k)  { this.serviceKey = k; }
}