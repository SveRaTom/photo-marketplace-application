package photomarketplace.model.dto.photo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoRequestDTO {

    @Size(max = 100, message = "Photo title cannot be longer than 100 characters")
    private String title;

    @NotBlank(message = "Photo URL must not be blank")
    @Size(max = 500, message = "Photo URL cannot be longer than 500 characters")
    private String imageUrl;

    @Size(max = 1000, message = "Photo description cannot be longer than 1000 characters")
    private String description;

    private boolean coverPhoto;
}
