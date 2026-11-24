package in.nalamcamp.Nalamcamp;

import jakarta.persistence.*;

@Entity
@Table(name = "organizers")
public class Organizer {

    public enum OrganizerType {
        GOVERNMENT, PRIVATE, NGO, JOINT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "organizer_type", nullable = false, length = 20)
    private OrganizerType organizerType;

    @Column(name = "official_contact", nullable = false)
    private String officialContact;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OrganizerType getOrganizerType() {
        return organizerType;
    }

    public void setOrganizerType(OrganizerType organizerType) {
        this.organizerType = organizerType;
    }

    public String getOfficialContact() {
        return officialContact;
    }

    public void setOfficialContact(String officialContact) {
        this.officialContact = officialContact;
    }
}
