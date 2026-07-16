class BookNotFoundError(Exception):
    def __init__(self, book_id: int):
        self.book_id = book_id
        super().__init__(f"Book with id {book_id} not found")