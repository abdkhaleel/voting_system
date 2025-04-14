package voting;

import java.io.Serializable;
import java.util.UUID;

public class Candidate implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String id;
    private String name;
    private String party;
    private String position;
    private String biography;
    private String imageUrl;
    
    public Candidate(String name, String party, String position) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.party = party;
        this.position = position;
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getParty() {
		return party;
	}

	public void setParty(String party) {
		this.party = party;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public String getBiography() {
		return biography;
	}

	public void setBiography(String biography) {
		this.biography = biography;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getId() {
		return id;
	}
    
}
