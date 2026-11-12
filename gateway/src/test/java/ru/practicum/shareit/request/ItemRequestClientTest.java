package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.practicum.shareit.common.ShareItHeaders;
import ru.practicum.shareit.request.dto.NewItemRequestDto;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(ItemRequestClient.class)
class ItemRequestClientTest {

    @Autowired
    private ItemRequestClient requestClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void shouldCallEveryRequestEndpointOfServer() {
        server.expect(requestTo("http://localhost:9090/requests"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(ShareItHeaders.USER_ID, "1"))
                .andExpect(content().json("{\"description\":\"Нужна дрель\"}"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":1}"));
        server.expect(requestTo("http://localhost:9090/requests"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:9090/requests/all"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:9090/requests/1"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        requestClient.create(1L, new NewItemRequestDto("Нужна дрель"));
        requestClient.getOwn(1L);
        requestClient.getAllOthers(1L);
        requestClient.getById(1L, 1L);

        server.verify();
    }
}
