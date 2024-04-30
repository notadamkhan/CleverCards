# CleverCards

CleverCards is a simple flashcard application. It is designed to help users learn and memorize information more effectively. The application allows users to create, edit, and delete flashcards, as well as quiz themselves on the flashcards they have created.

## App Screenshots
<div align="center">
  <div style="display: flex; justify-content: center; margin-bottom: 20px;">
    <img src="img1.png" alt="Screenshot 1" width="200" style="margin-right: 20px;">
    <img src="img2.png" alt="Screenshot 2" width="200">
    <img src="img3.png" alt="Screenshot 3" width="200" style="margin-right: 20px;">
    <img src="img4.png" alt="Screenshot 4" width="200">
  </div>
</div>

## Roadmap (if I had more time)
- Add 'create new quiz' card to home page at end of quiz grid, or if there is no quiz grid, first on the page.
- Show a success snackbar message on the home page when a quiz is created. add a button to practice that quiz on the snackbar message.
- Handle all errors better
- Success snackbar on quiz creation

## Known Issues
- Error snackbars on 'Create' tab are not dismissible
- Image generation is a little slow (api limitation)
- Image generation might yield generic images if request breaks OpenAI TOS
- Navigation is a little buggy when going from Create to Home and Practice to Home. Takes two taps.