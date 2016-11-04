package view.service;

import retrofit2.http.GET;
import rx.Observable;
import view.dto.ImageDTO;

/**
 * Created by lucas on 11/4/16.
 */

public interface PictureService {
    @GET("data/imgs?col=壁纸&tag=全部&sort=0&pn=1&rn=10&p=channel&from=1")
    Observable<ImageDTO> getPicture();
}