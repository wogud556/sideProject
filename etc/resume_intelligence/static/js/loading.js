(function () {
    var overlay = document.getElementById("loading-overlay");
    if (!overlay) {
        return;
    }

    function showOverlay() {
        overlay.classList.add("active");
    }

    function hideOverlay() {
        overlay.classList.remove("active");
    }

    // 모든 폼 제출(업로드/재분석/DART 검색·연결 등) 시 오버레이 표시.
    // submit 이벤트는 브라우저 기본 검증을 통과한 뒤에만 발생한다.
    document.addEventListener("submit", function () {
        showOverlay();
    });

    // 서버에서 DART API를 호출하는 링크는 data-loading 속성으로 표시.
    document.addEventListener("click", function (event) {
        var link = event.target.closest("a[data-loading]");
        if (link) {
            showOverlay();
        }
    });

    // 뒤로가기(bfcache) 복귀 시 오버레이가 남아있지 않도록 해제.
    window.addEventListener("pageshow", function (event) {
        if (event.persisted) {
            hideOverlay();
        }
    });
})();
