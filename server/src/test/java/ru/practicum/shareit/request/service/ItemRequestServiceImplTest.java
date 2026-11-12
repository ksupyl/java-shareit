package ru.practicum.shareit.request.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemShortDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.NewItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemRequestServiceImplTest {

    @Mock
    private ItemRequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemRequestServiceImpl requestService;

    private final User author = User.builder().id(1L).name("Ksenia").email("ksenia@mail.ru").build();
    private final User owner = User.builder().id(2L).name("Owner").email("owner@mail.ru").build();

    @Test
    void shouldCreateRequestWithoutItems() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        // база выдаёт id при сохранении — мок делает то же самое
        when(requestRepository.save(any(ItemRequest.class))).thenAnswer(invocation -> {
            ItemRequest request = invocation.getArgument(0);
            request.setId(10L);
            return request;
        });

        ItemRequestDto result = requestService.create(1L, new NewItemRequestDto("Нужна дрель"));

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getDescription()).isEqualTo("Нужна дрель");
        assertThat(result.getCreated()).isNotNull();
        assertThat(result.getItems()).isEmpty();
        ArgumentCaptor<ItemRequest> saved = ArgumentCaptor.forClass(ItemRequest.class);
        verify(requestRepository).save(saved.capture());
        assertThat(saved.getValue().getRequestor()).isEqualTo(author);
    }

    @Test
    void shouldNotCreateRequestOfUnknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.create(99L, new NewItemRequestDto("Нужна дрель")))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
        verify(requestRepository, never()).save(any());
    }

    @Test
    void shouldReturnOwnRequestsNewestFirstWithTheirItems() {
        ItemRequest newer = request(11L, "Нужна стремянка");
        ItemRequest older = request(10L, "Нужна дрель");
        Item ladder = Item.builder().id(5L).name("Стремянка").owner(owner).request(newer).build();
        when(userRepository.existsById(1L)).thenReturn(true);
        when(requestRepository.findByRequestor_Id(eq(1L), any(Sort.class))).thenReturn(List.of(newer, older));
        when(itemRepository.findByRequest_IdInOrderById(List.of(11L, 10L))).thenReturn(List.of(ladder));

        List<ItemRequestDto> result = requestService.getOwn(1L);

        assertThat(result).extracting(ItemRequestDto::getId).containsExactly(11L, 10L);
        assertThat(result.get(0).getItems()).containsExactly(new ItemShortDto(5L, "Стремянка", 2L));
        assertThat(result.get(1).getItems()).isEmpty();
        // порядок списка задаёт база: сервис просит у неё сначала новые, а при равном времени — больший id
        ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
        verify(requestRepository).findByRequestor_Id(eq(1L), sort.capture());
        assertThat(sort.getValue()).isEqualTo(Sort.by(Sort.Direction.DESC, "created", "id"));
    }

    @Test
    void shouldNotQueryItemsWhenThereAreNoRequests() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(requestRepository.findByRequestor_Id(eq(1L), any(Sort.class))).thenReturn(List.of());

        assertThat(requestService.getOwn(1L)).isEmpty();
        verifyNoInteractions(itemRepository);
    }

    @Test
    void shouldReturnRequestsOfOtherUsers() {
        ItemRequest foreign = request(12L, "Нужен велосипед");
        when(userRepository.existsById(1L)).thenReturn(true);
        when(requestRepository.findByRequestor_IdNot(eq(1L), any(Sort.class))).thenReturn(List.of(foreign));
        when(itemRepository.findByRequest_IdInOrderById(List.of(12L))).thenReturn(List.of());

        List<ItemRequestDto> result = requestService.getAllOthers(1L);

        assertThat(result).extracting(ItemRequestDto::getDescription).containsExactly("Нужен велосипед");
        assertThat(result.get(0).getItems()).isEmpty();
    }

    @Test
    void shouldReturnRequestByIdToAnyUser() {
        ItemRequest request = request(10L, "Нужна дрель");
        Item drill = Item.builder().id(7L).name("Дрель").owner(owner).request(request).build();
        when(userRepository.existsById(2L)).thenReturn(true);
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(itemRepository.findByRequest_IdInOrderById(List.of(10L))).thenReturn(List.of(drill));

        ItemRequestDto result = requestService.getById(2L, 10L);

        assertThat(result.getDescription()).isEqualTo("Нужна дрель");
        assertThat(result.getItems()).containsExactly(new ItemShortDto(7L, "Дрель", 2L));
    }

    @Test
    void shouldThrowWhenRequestNotFound() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.getById(1L, 99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void shouldNotShowRequestsToUnknownUser() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> requestService.getOwn(99L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> requestService.getAllOthers(99L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> requestService.getById(99L, 10L)).isInstanceOf(NotFoundException.class);
        verify(requestRepository, never()).findById(anyLong());
    }

    private ItemRequest request(Long id, String description) {
        return ItemRequest.builder()
                .id(id)
                .description(description)
                .requestor(author)
                .created(LocalDateTime.of(2026, 11, 1, 12, 0).plusMinutes(id))
                .build();
    }
}
