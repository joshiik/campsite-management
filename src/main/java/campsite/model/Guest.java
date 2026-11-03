package campsite.model;

import java.io.Serializable;
import java.time.LocalDate;

public final class Guest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String name;
    private final LocalDate dateOfBirth;
    private final String address;
    private final String telephone;
    private final String email;
    private final String notes;

    public Guest(int id, String name, LocalDate dateOfBirth, String address,
            String telephone, String email, String notes) {
        this.id = id;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.telephone = telephone;
        this.email = email;
        this.notes = notes;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public LocalDate dateOfBirth() {
        return dateOfBirth;
    }

    public String address() {
        return address;
    }

    public String telephone() {
        return telephone;
    }

    public String email() {
        return email;
    }

    public String notes() {
        return notes;
    }

    @Override
    public String toString() {
        return id + " - " + name;
    }
}
