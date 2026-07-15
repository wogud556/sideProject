from django.urls import path

from . import views

app_name = "dart"

urlpatterns = [
    path(
        "resumes/<uuid:resume_id>/careers/<int:index>/search/",
        views.career_search_view,
        name="career_search",
    ),
    path("corporations/<str:corp_code>/", views.company_detail_view, name="company_detail"),
]
