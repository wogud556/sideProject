from django.urls import path

from . import views

app_name = "resumes_api"

urlpatterns = [
    path("resumes", views.resume_list_create_view, name="resume_list_create"),
    path("resumes/<uuid:resume_id>", views.resume_detail_view, name="resume_detail"),
    path("resumes/<uuid:resume_id>/status", views.resume_status_view, name="resume_status"),
    path("resumes/<uuid:resume_id>/profile", views.resume_profile_patch_view, name="resume_profile_patch"),
    path("resumes/<uuid:resume_id>/confirm", views.resume_confirm_view, name="resume_confirm"),
    path("resumes/<uuid:resume_id>/reanalyze", views.resume_reanalyze_view, name="resume_reanalyze"),
    path("resumes/<uuid:resume_id>/excel", views.resume_excel_view, name="resume_excel"),
    path("resumes/<uuid:resume_id>/careers", views.career_list_create_view, name="career_list_create"),
    path("resumes/<uuid:resume_id>/careers/<int:index>", views.career_detail_view, name="career_detail"),
    path("resumes/<uuid:resume_id>/educations", views.education_list_create_view, name="education_list_create"),
    path("resumes/<uuid:resume_id>/educations/<int:index>", views.education_detail_view, name="education_detail"),
]
