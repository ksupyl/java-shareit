package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.practicum.shareit.common.ShareItHeaders;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(ItemClient.class)
class ItemClientTest {

    @Autowired
    private ItemClient itemClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void shouldCreateAndUpdateItemOnBehalfOfUser() {
        server.expect(requestTo("http://localhost:9090/items"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(ShareItHeaders.USER_ID, "1"))
                .andExpect(content().json("{\"name\":\"Дрель\",\"requestId\":3}"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":10}"));
        server.expect(requestTo("http://localhost:9090/items/10"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(ShareItHeaders.USER_ID, "1"))
                .andRespond(withSuccess("{\"id\":10}", MediaType.APPLICATION_JSON));

        itemClient.create(1L, ItemDto.builder().name("Дрель").description("Ударная").available(true)
                .requestId(3L).build());
        itemClient.update(1L, 10L, ItemDto.builder().available(false).build());

        server.verify();
    }

    @Test
    void shouldNotSendUserHeaderWhenItIsUnknown() {
        server.expect(requestTo("http://localhost:9090/items/10"))
                .andExpect(headerDoesNotExist(ShareItHeaders.USER_ID))
                .andRespond(withSuccess("{\"id\":10}", MediaType.APPLICATION_JSON));

        assertThat(itemClient.getById(null, 10L).getStatusCode()).isEqualTo(HttpStatus.OK);
        server.verify();
    }

    @Test
    void shouldEncodeSearchText() {
        // & и пробел закодированы: иначе сервер увидел бы text=a и ещё один параметр
        server.expect(requestTo("http://localhost:9090/items/search?text=a%26b%20c"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        itemClient.search("a&b c");

        server.verify();
    }

    @Test
    void shouldReadOwnerItemsAndAddComment() {
        server.expect(requestTo("http://localhost:9090/items"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(ShareItHeaders.USER_ID, "1"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:9090/items/10/comment"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"text\":\"Отличная дрель\"}"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":1}"));

        itemClient.getAllByOwner(1L);
        itemClient.addComment(2L, 10L, new CommentDto("Отличная дрель"));

        server.verify();
    }
}
