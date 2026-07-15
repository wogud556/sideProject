from django import forms


class ResumeUploadForm(forms.Form):
    file = forms.FileField(label="이력서 PDF")
