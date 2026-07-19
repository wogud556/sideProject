document.getElementById('contact-email').addEventListener('click', (e) => {
  e.preventDefault();
  const email = 'wogud556@naver.com';

  navigator.clipboard.writeText(email).then(() => {
    const el = e.currentTarget;
    const original = el.textContent;
    el.textContent = '이메일이 복사되었습니다!';
    setTimeout(() => {
      el.textContent = original;
    }, 1500);
  });
});
